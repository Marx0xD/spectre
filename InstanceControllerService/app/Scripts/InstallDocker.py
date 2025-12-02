def IntializationScript(username: str):
    return f"""#!/bin/bash
set -euo pipefail

# --- Docker setup (unchanged functional intent) ---
sudo dnf remove -y docker docker-client docker-client-latest docker-common docker-latest docker-latest-logrotate docker-logrotate docker-engine || true
sudo dnf -y install dnf-plugins-core
sudo dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo systemctl enable --now docker
# Ensure the TARGET user (not $USER running this script) can use docker
if id -u {username} >/dev/null 2>&1; then
  sudo usermod -aG docker {username} || true
fi

echo "==== VERIFYING DOCKER INSTALLATION ===="
if ! command -v docker &>/dev/null; then
  echo "ERROR: docker command not found in PATH"
  exit 1
fi
docker --version || exit 1
echo "SUCCESS: Docker CLI is installed"

# --- Ensure ACL tools available (for default perms) ---
sudo dnf install -y acl || true

# --- Writable /opt/ContainerConfiguration for {username} ---
# 1) Create a cooperative group so multiple users can collaborate if desired
sudo groupadd -f containercfg

# 2) Create the base dir (setgid bit so new subdirs inherit group)
sudo install -d -m 2775 -o {username} -g containercfg /opt/ContainerConfiguration

# 3) Make sure {username} is in the group (no-op if already is)
sudo usermod -aG containercfg {username} || true

# 4) Grant immediate and default ACLs so the user AND group have rwx going forward
sudo setfacl -R -m u:{username}:rwx /opt/ContainerConfiguration
sudo setfacl -R -m g:containercfg:rwx /opt/ContainerConfiguration
sudo setfacl -R -m d:u:{username}:rwx /opt/ContainerConfiguration
sudo setfacl -R -m d:g:containercfg:rwx /opt/ContainerConfiguration

# (Optional) If you want others to be able to read/enter:
# sudo setfacl -R -m o:rx /opt/ContainerConfiguration
# sudo setfacl -R -m d:o:rx /opt/ContainerConfiguration

# 5) SELinux label suitable for container bind-mounts
sudo chcon -R -t container_file_t /opt/ContainerConfiguration || true

# 6) Smoke test: ensure the target user can write now
sudo -u {username} bash -lc 'touch /opt/ContainerConfiguration/.perm_check && rm -f /opt/ContainerConfiguration/.perm_check'
echo "Writable setup for {username} at /opt/ContainerConfiguration is ready."

# Optionally prep a test instance dir (inherits group + ACLs)
sudo install -d -m 2775 -o {username} -g containercfg /opt/ContainerConfiguration/test-container
"""


def startup_container(instance_location):
    return f"""
    cd {instance_location}
    docker compose up -d
    
    """