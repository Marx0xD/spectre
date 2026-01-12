#!/bin/bash
set -euo pipefail

# --------------------------------------------------------------------
# Ubuntu Host Initialization Script
# Variables expected:
#   USERNAME – system user to configure
# --------------------------------------------------------------------

if [ -z "${USERNAME:-}" ]; then
  echo "ERROR: USERNAME variable is not set."
  exit 1
fi

echo "=== Starting Initialization for Ubuntu Host (user=$USERNAME) ==="

# --------------------------------------------------------------------
# Remove any conflicting Docker versions
# --------------------------------------------------------------------
sudo apt-get remove -y docker docker-engine docker.io containerd runc || true

sudo apt-get update -y
sudo apt-get install -y ca-certificates curl gnupg lsb-release acl

# --------------------------------------------------------------------
# Install Docker CE from official repository
# --------------------------------------------------------------------
sudo install -m 0755 -d /etc/apt/keyrings

sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc

sudo chmod a+r /etc/apt/keyrings/docker.asc

# Determine Ubuntu codename (fallback if needed)
UBUNTU_CODENAME=$(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")

sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: ${UBUNTU_CODENAME}
Components: stable
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt-get update -y
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo systemctl enable --now docker

# Add target user to docker group
if id -u "$USERNAME" >/dev/null 2>&1; then
    sudo usermod -aG docker "$USERNAME" || true
fi

echo "==== VERIFYING DOCKER INSTALLATION ===="
if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker command not found"
  exit 1
fi

docker --version
echo "SUCCESS: Docker CLI installed."

# --------------------------------------------------------------------
# Create and ACL-configure /opt/ContainerConfiguration
# --------------------------------------------------------------------

sudo groupadd -f containercfg

sudo install -d -m 2775 -o "$USERNAME" -g containercfg /opt/ContainerConfiguration

sudo usermod -aG containercfg "$USERNAME" || true

# Apply ACLs: user + shared group full access; default ACLs also
sudo setfacl -R -m u:"$USERNAME":rwx /opt/ContainerConfiguration
sudo setfacl -R -m g:containercfg:rwx /opt/ContainerConfiguration
sudo setfacl -R -m d:u:"$USERNAME":rwx /opt/ContainerConfiguration
sudo setfacl -R -m d:g:containercfg:rwx /opt/ContainerConfiguration

# Ubuntu typically uses AppArmor, not SELinux — no chcon equivalent needed.

# Check if user can write
sudo -u "$USERNAME" bash -lc 'touch /opt/ContainerConfiguration/.perm_check && rm -f /opt/ContainerConfiguration/.perm_check'
echo "Permissions verified: $USERNAME can write to /opt/ContainerConfiguration."

# Optional: prepare sample container dir
sudo install -d -m 2775 -o "$USERNAME" -g containercfg /opt/ContainerConfiguration/test-container

echo "=== Ubuntu Initialization Complete for user=$USERNAME ==="
