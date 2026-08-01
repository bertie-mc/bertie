{
  description = "Bertie modpack, custom mods, and test tooling";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

    flake-parts = {
      url = "github:hercules-ci/flake-parts";
      inputs.nixpkgs-lib.follows = "nixpkgs";
    };

    treefmt-nix = {
      url = "github:numtide/treefmt-nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs =
    inputs@{
      self,
      flake-parts,
      ...
    }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = [
        "x86_64-linux"
        "aarch64-linux"
      ];

      imports = [ inputs.treefmt-nix.flakeModule ];

      perSystem =
        {
          config,
          lib,
          pkgs,
          ...
        }:
        let
          bertie-ci = pkgs.callPackage ./tools/bertie-ci/nix/package.nix {
            bertie-pack = ./pack;
          };
          ci-tools = pkgs.buildEnv {
            name = "bertie-ci-tools";
            paths = [
              bertie-ci
              pkgs.python3
            ];
          };
          maintainedPython = [
            ".github/actions/**/*.py"
            "tools/bertie-ci/**/*.py"
          ];
          app = {
            program = bertie-ci;
            meta.description = bertie-ci.meta.description;
          };
        in
        {
          packages = {
            default = bertie-ci;
            inherit bertie-ci ci-tools;
          };

          apps = {
            default = app;
            bertie-ci = app;
          };

          checks = {
            package = bertie-ci;
            help =
              pkgs.runCommand "bertie-ci-help"
                {
                  nativeBuildInputs = [ bertie-ci ];
                }
                ''
                  bertie-ci --help > "$out"
                '';
            workflows =
              pkgs.runCommand "bertie-workflows"
                {
                  source = self.outPath;
                  nativeBuildInputs = [
                    pkgs.action-validator
                    pkgs.actionlint
                  ];
                }
                ''
                  cd "$source"
                  actionlint .github/workflows/*.yml
                  action-validator .github/actions/*/action.yml .github/workflows/*.yml
                  touch "$out"
                '';
          };

          devShells.default = pkgs.mkShellNoCC {
            packages = [
              config.treefmt.build.wrapper
              bertie-ci
              pkgs.action-validator
              pkgs.actionlint
              pkgs.curl
              pkgs.git
              pkgs.gh
              pkgs.gradle_8
              pkgs.jdk21
              pkgs.jq
              pkgs.packwiz
              pkgs.python3
              pkgs.python3Packages.pytest
              pkgs.ruff
              pkgs.unzip
              pkgs.zip
            ];
            JAVA_HOME = "${pkgs.jdk21}";
          };

          treefmt.programs = {
            nixfmt.enable = true;
            ruff-check = {
              enable = true;
              extendSelect = [ "I" ];
            };
            ruff-format.enable = true;
          };
          treefmt.settings.formatter = {
            ruff-check.includes = lib.mkForce maintainedPython;
            ruff-format.includes = lib.mkForce maintainedPython;
          };
        };
    };
}
