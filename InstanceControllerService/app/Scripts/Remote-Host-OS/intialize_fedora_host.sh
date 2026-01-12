#!/bin/bash
set -euo pipefail

# --------------------------------------------------------------------
# Remote-Host-OS Host Initialization Script
# Variables expected:
#   USERNAME   - the system user to grant Docker + directory permissions
# --------------------------------------------------------------------

if [ -z "${USERNAME:-}" ]; then
  echo "ERROR: USERNAME variable is not set."
  exit 1
fi

echo "=== Starting Initialization for Fedora Host (user=$USERNAME) ==="

# --------------------------------------------------------------------
# Remove old Docker installations
# --------------------------------------------------------------------
sudo dnf remove -y docker docker-client docker-client-latest docker-common \
  docker-latest docker-latest-logrotate docker-logrotate docker-engine || true

# Install Docker CE repositories + packages
sudo dnf -y install dnf-plugins-core
sudo dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo systemctl enable --now docker

# Add target user to docker group
if id -u "$USERNAME" >/dev/null 2>&1; then
    sudo usermod -aG docker "$USERNAME" || true
fi

echo "==== VERIFYING DOCKER INSTALLATION ===="
if ! command -v docker &>/dev/null; then
  echo "ERROR: Docker command not found in PATH"
  exit 1
fi
docker --version
echo "SUCCESS: Docker CLI is installed."

# --------------------------------------------------------------------
# ACL tools required for permission inheritance
# --------------------------------------------------------------------
sudo dnf install -y acl || true

# --------------------------------------------------------------------
# Prepare /opt/ContainerConfiguration
# --------------------------------------------------------------------
sudo groupadd -f containercfg

# Create base directory with setgid (for shared group inheritance)
sudo install -d -m 2775 -o "$USERNAME" -g containercfg /opt/ContainerConfiguration

# Ensure the user is in the containercfg group
sudo usermod -aG containercfg "$USERNAME" || true

# Apply ACLs (read/write/execute) + default ACLs
sudo setfacl -R -m u:"$USERNAME":rwx /opt/ContainerConfiguration
sudo setfacl -R -m g:containercfg:rwx /opt/ContainerConfiguration
sudo setfacl -R -m d:u:"$USERNAME":rwx /opt/ContainerConfiguration
sudo setfacl -R -m d:g:containercfg:rwx /opt/ContainerConfiguration

# SELinux label to allow bind mounts
sudo chcon -R -t container_file_t /opt/ContainerConfiguration || true

# Verify write access
sudo -u "$USERNAME" bash -lc 'touch /opt/ContainerConfiguration/.perm_check && rm -f /opt/ContainerConfiguration/.perm_check'
echo "Permissions verified: $USERNAME can write to /opt/ContainerConfiguration."

# Optional: create a test instance directory
sudo install -d -m 2775 -o "$USERNAME" -g containercfg /opt/ContainerConfiguration/test-container

echo "=== Fedora Initialization Complete for user=$USERNAME ==="
