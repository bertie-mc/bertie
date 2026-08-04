{
  lib,
  stdenv,
  pkg-config,
  python3Packages,
  fetchurl,
  bash,
  coreutils,
  git,
  gradle_8,
  jdk21,
  packwiz,
  sway,
  wlroots,
  glfw3-minecraft,
  alsa-lib,
  libglvnd,
  libpulseaudio,
  flite,
  wayland,
  wayland-scanner,
  libxkbcommon,
  libdecor,
  mesa,
  openal,
}:
let
  pyproject = lib.importTOML ../pyproject.toml;
  versions = lib.importJSON ../versions.json;

  packwizInstaller = fetchurl {
    inherit (versions.packwiz_installer) url hash;
  };

  seatKeyboard = stdenv.mkDerivation {
    pname = "bertie-wayland-seat-keyboard";
    version = pyproject.project.version;
    src = ../native;

    nativeBuildInputs = [
      pkg-config
      wayland-scanner
    ];
    buildInputs = [
      wayland
      libxkbcommon
    ];

    buildPhase = ''
      runHook preBuild
      protocol=${wlroots.src}/protocol/virtual-keyboard-unstable-v1.xml
      ${lib.getExe wayland-scanner} client-header "$protocol" virtual-keyboard-unstable-v1-client-protocol.h
      ${lib.getExe wayland-scanner} private-code "$protocol" virtual-keyboard-unstable-v1-protocol.c
      $CC -std=c11 -Wall -Wextra -Werror \
        $(pkg-config --cflags wayland-client xkbcommon) \
        wayland-seat-keyboard.c virtual-keyboard-unstable-v1-protocol.c \
        $(pkg-config --libs wayland-client xkbcommon) \
        -o bertie-wayland-seat-keyboard
      runHook postBuild
    '';

    installPhase = ''
      runHook preInstall
      install -Dm755 bertie-wayland-seat-keyboard "$out/bin/bertie-wayland-seat-keyboard"
      runHook postInstall
    '';

    meta = {
      description = "Persistent virtual keyboard for bertie-ci's isolated Wayland seat";
      license = [
        lib.licenses.unlicense
        lib.licenses.mit
      ];
      platforms = lib.platforms.linux;
      mainProgram = "bertie-wayland-seat-keyboard";
    };
  };

  runtimePath = lib.makeBinPath [
    bash
    coreutils
    git
    gradle_8
    jdk21
    packwiz
    seatKeyboard
    sway
  ];
  runtimeLibraryPath = lib.makeLibraryPath [
    alsa-lib
    libglvnd
    libpulseaudio
    flite
    wayland
    libxkbcommon
    libdecor
    mesa
    openal
  ];
in
python3Packages.buildPythonApplication {
  pname = pyproject.project.name;
  inherit (pyproject.project) version;
  pyproject = true;

  src = lib.fileset.toSource {
    root = ../.;
    fileset = lib.fileset.unions [
      ../UNLICENSE
      ../pyproject.toml
      ../src
      ../tests
    ];
  };

  build-system = [ python3Packages.setuptools ];

  nativeCheckInputs = [
    git
    python3Packages.pytestCheckHook
  ];
  __structuredAttrs = true;
  makeWrapperArgs = [
    "--prefix"
    "PATH"
    ":"
    runtimePath
    "--set-default"
    "BERTIE_CI_GRADLE"
    (lib.getExe gradle_8)
    "--set-default"
    "BERTIE_CI_PACKWIZ_INSTALLER_JAR"
    packwizInstaller
    "--set-default"
    "BERTIE_CI_PACKWIZ"
    (lib.getExe packwiz)
    "--set-default"
    "BERTIE_CI_JAVA_HOME"
    jdk21
    "--set-default"
    "BERTIE_CI_SWAY"
    (lib.getExe sway)
    "--set-default"
    "BERTIE_CI_WAYLAND_SEAT_KEYBOARD"
    (lib.getExe seatKeyboard)
    "--set-default"
    "BERTIE_CI_WAYLAND_GLFW"
    "${glfw3-minecraft}/lib/libglfw.so"
    "--set-default"
    "BERTIE_CI_WAYLAND_GL_DRIVERS_PATH"
    "${mesa}/lib/dri"
    "--set-default"
    "BERTIE_CI_WAYLAND_EGL_VENDOR_LIBRARY_FILENAMES"
    "${mesa}/share/glvnd/egl_vendor.d/50_mesa.json"
    "--set-default"
    "BERTIE_CI_WAYLAND_LIBRARY_PATH"
    runtimeLibraryPath
  ];

  pythonImportsCheck = [ "bertie_ci" ];

  meta = {
    description = pyproject.project.description;
    homepage = "https://github.com/bertie-mc/bertie/tree/main/tools/bertie-ci";
    license = lib.licenses.unlicense;
    mainProgram = "bertie-ci";
    platforms = lib.platforms.linux;
  };
}
