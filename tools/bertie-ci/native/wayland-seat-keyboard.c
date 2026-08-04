#define _GNU_SOURCE

#include <errno.h>
#include <poll.h>
#include <signal.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

#include <wayland-client.h>
#include <xkbcommon/xkbcommon.h>

#include "virtual-keyboard-unstable-v1-client-protocol.h"

struct globals {
    struct wl_seat *seat;
    struct zwp_virtual_keyboard_manager_v1 *manager;
};

static volatile sig_atomic_t stopping;

static void request_stop(int signal_number)
{
    (void) signal_number;
    stopping = 1;
}

static void global_added(void *data, struct wl_registry *registry, uint32_t name,
                         const char *interface, uint32_t version)
{
    struct globals *globals = data;
    (void) version;

    if (globals->seat == NULL && strcmp(interface, wl_seat_interface.name) == 0) {
        globals->seat = wl_registry_bind(registry, name, &wl_seat_interface, 1);
    } else if (globals->manager == NULL &&
               strcmp(interface, zwp_virtual_keyboard_manager_v1_interface.name) == 0) {
        globals->manager = wl_registry_bind(
            registry, name, &zwp_virtual_keyboard_manager_v1_interface, 1);
    }
}

static void global_removed(void *data, struct wl_registry *registry, uint32_t name)
{
    (void) data;
    (void) registry;
    (void) name;
}

static const struct wl_registry_listener registry_listener = {
    .global = global_added,
    .global_remove = global_removed,
};

static int install_keymap(struct zwp_virtual_keyboard_v1 *keyboard)
{
    static const struct xkb_rule_names names = {
        .rules = "evdev",
        .model = "pc105",
        .layout = "us",
    };
    struct xkb_context *context = NULL;
    struct xkb_keymap *keymap = NULL;
    char *text = NULL;
    int descriptor = -1;
    int result = -1;

    context = xkb_context_new(XKB_CONTEXT_NO_FLAGS);
    if (context == NULL) {
        fputs("failed to create an XKB context\n", stderr);
        goto cleanup;
    }
    keymap = xkb_keymap_new_from_names(context, &names, XKB_KEYMAP_COMPILE_NO_FLAGS);
    if (keymap == NULL) {
        fputs("failed to compile the evdev/pc105/us XKB keymap\n", stderr);
        goto cleanup;
    }
    text = xkb_keymap_get_as_string(keymap, XKB_KEYMAP_FORMAT_TEXT_V1);
    if (text == NULL) {
        fputs("failed to serialize the XKB keymap\n", stderr);
        goto cleanup;
    }

    size_t size = strlen(text) + 1;
    if (size > UINT32_MAX) {
        fputs("serialized XKB keymap is too large\n", stderr);
        goto cleanup;
    }
    descriptor = memfd_create("bertie-ci-xkb-keymap", MFD_CLOEXEC);
    if (descriptor < 0) {
        perror("memfd_create");
        goto cleanup;
    }

    size_t offset = 0;
    while (offset < size) {
        ssize_t written = write(descriptor, text + offset, size - offset);
        if (written < 0) {
            if (errno == EINTR) {
                continue;
            }
            perror("write XKB keymap");
            goto cleanup;
        }
        offset += (size_t) written;
    }
    if (lseek(descriptor, 0, SEEK_SET) < 0) {
        perror("rewind XKB keymap");
        goto cleanup;
    }

    zwp_virtual_keyboard_v1_keymap(keyboard, WL_KEYBOARD_KEYMAP_FORMAT_XKB_V1,
                                   descriptor, (uint32_t) size);
    result = 0;

cleanup:
    if (descriptor >= 0) {
        close(descriptor);
    }
    free(text);
    xkb_keymap_unref(keymap);
    xkb_context_unref(context);
    return result;
}

static int wait_for_shutdown(struct wl_display *display)
{
    struct pollfd descriptors[] = {
        {.fd = STDIN_FILENO, .events = POLLIN},
        {.fd = wl_display_get_fd(display), .events = POLLIN},
    };

    while (!stopping) {
        int ready = poll(descriptors, 2, -1);
        if (ready < 0) {
            if (errno == EINTR) {
                continue;
            }
            perror("poll");
            return -1;
        }

        if (descriptors[0].revents & (POLLIN | POLLHUP)) {
            char discard[256];
            ssize_t count = read(STDIN_FILENO, discard, sizeof(discard));
            if (count == 0) {
                return 0;
            }
            if (count < 0 && errno != EINTR) {
                perror("read stdin");
                return -1;
            }
        }
        if (descriptors[1].revents & (POLLERR | POLLHUP | POLLNVAL)) {
            fputs("Wayland compositor disconnected\n", stderr);
            return -1;
        }
        if (descriptors[1].revents & POLLIN && wl_display_dispatch(display) < 0) {
            fputs("failed to dispatch Wayland events\n", stderr);
            return -1;
        }
    }
    return 0;
}

int main(void)
{
    struct sigaction action = {.sa_handler = request_stop};
    struct wl_display *display = NULL;
    struct wl_registry *registry = NULL;
    struct zwp_virtual_keyboard_v1 *keyboard = NULL;
    struct globals globals = {0};
    int result = EXIT_FAILURE;

    sigemptyset(&action.sa_mask);
    if (sigaction(SIGINT, &action, NULL) < 0 || sigaction(SIGTERM, &action, NULL) < 0) {
        perror("sigaction");
        return EXIT_FAILURE;
    }

    display = wl_display_connect(NULL);
    if (display == NULL) {
        fputs("failed to connect to the Wayland compositor\n", stderr);
        goto cleanup;
    }
    registry = wl_display_get_registry(display);
    if (registry == NULL || wl_registry_add_listener(registry, &registry_listener,
                                                     &globals) < 0 ||
        wl_display_roundtrip(display) < 0) {
        fputs("failed to discover Wayland globals\n", stderr);
        goto cleanup;
    }
    if (globals.seat == NULL) {
        fputs("the Wayland compositor did not advertise a wl_seat\n", stderr);
        goto cleanup;
    }
    if (globals.manager == NULL) {
        fputs("the Wayland compositor did not advertise virtual-keyboard-v1\n", stderr);
        goto cleanup;
    }

    keyboard = zwp_virtual_keyboard_manager_v1_create_virtual_keyboard(
        globals.manager, globals.seat);
    if (keyboard == NULL || install_keymap(keyboard) < 0 ||
        wl_display_roundtrip(display) < 0) {
        fputs("failed to attach the virtual keyboard\n", stderr);
        goto cleanup;
    }

    puts("ready");
    fflush(stdout);
    result = wait_for_shutdown(display) == 0 ? EXIT_SUCCESS : EXIT_FAILURE;

cleanup:
    if (keyboard != NULL) {
        zwp_virtual_keyboard_v1_destroy(keyboard);
    }
    if (globals.manager != NULL) {
        zwp_virtual_keyboard_manager_v1_destroy(globals.manager);
    }
    if (globals.seat != NULL) {
        wl_seat_destroy(globals.seat);
    }
    if (registry != NULL) {
        wl_registry_destroy(registry);
    }
    if (display != NULL) {
        wl_display_disconnect(display);
    }
    return result;
}
