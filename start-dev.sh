#!/bin/bash
# ============================================================
# VCogniRule - Development Launcher (Mac/Linux)
# ============================================================
# Assumes Java 21+ and Maven 3.9+ are installed via your
# system package manager and available in PATH.
# ============================================================

echo "============================================================"
echo " VCogniRule - Development Environment
echo "============================================================"
echo ""

# Verify Java
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java not found in PATH."
    echo ""
    echo "Install JDK 21+ using your package manager:"
    echo "  macOS:   brew install openjdk@21"
    echo "  Ubuntu:  sudo apt install openjdk-21-jdk"
    echo "  Fedora:  sudo dnf install java-21-openjdk"
    echo "  Arch:    sudo pacman -S jdk-openjdk"
    echo ""
    exit 1
fi

# Verify Maven
if ! command -v mvn &> /dev/null; then
    echo "[ERROR] Maven not found in PATH."
    echo ""
    echo "Install Maven using your package manager:"
    echo "  macOS:   brew install maven"
    echo "  Ubuntu:  sudo apt install maven"
    echo "  Fedora:  sudo dnf install maven"
    echo "  Arch:    sudo pacman -S maven"
    echo ""
    exit 1
fi

echo "Java:"
java -version
echo ""
echo "Maven:"
mvn -version
echo ""
echo "Environment ready."
echo "Project: $(pwd)"
echo ""

# Open VS Code if available (optional)
if command -v code &> /dev/null; then
    echo "Opening VS Code..."
    code . &
fi

echo ""
echo "============================================================"
echo " Terminal is ready. Run:  mvn spring-boot:run"
echo " Then open: http://localhost:8080"
echo "============================================================"