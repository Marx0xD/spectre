from app.services import DeploymentEngineBase

import yaml
import os
import shutil
import posixpath, errno

class ConfigurationFileManager:
    def __init__(self,deployment_engine:DeploymentEngineBase,instance_name,instance_port,instance_database_port):
        self.deployment_engine = deployment_engine
        self.SOURCE = os.path.abspath("OdooConfigurationFiles")
        self.instance_name = instance_name
        self.instance_port = instance_port
        self.instance_database_port =instance_database_port



    def _mkdir_p(self, sftp, path):
        # create parents one by one; idempotent
        parts = [p for p in path.split("/") if p]  # drop extra slashes
        cur = "/" if path.startswith("/") else ""
        for p in parts:
            cur = (cur + p) if cur in ("", "/") else (cur + "/" + p)
            try:
                sftp.mkdir(cur)
            except IOError as e:
                # ignore "already exists"
                msg = str(e).lower()
                if "file exists" in msg or "failure" in msg:
                    continue
                raise

    def create_instance_file_data(self)-> str:
        print("moving conf")
        instance_config = f"/opt/ContainerConfiguration/{self.instance_name}/"
        container_port_mapping = [f'{self.instance_port}:8069']
        db_port_mapping = [f'{self.instance_database_port}:5432']
        container_config_file = shutil.copytree(self.SOURCE,instance_config,dirs_exist_ok=True)
        yaml_file = os.path.join(instance_config, 'docker-compose.yml')
        with open(yaml_file) as file:
            print(f"creating base config for instance {self.instance_name}")
            base_config = yaml.safe_load(file)
            base_config['services']['web']['ports'] = container_port_mapping
            base_config['services']['web']['container_name'] = self.instance_name
            base_config['services']['db']['container_name'] = self.instance_name + '_db'
            base_config['services']['db']['ports'] = db_port_mapping
        with open(yaml_file, 'w') as file:
            yaml.safe_dump(base_config, file, default_flow_style=False)
        return  container_config_file

    def upload_remote_dir(self, sftp_client, folder_path, remote_dest_dir):
        base = remote_dest_dir.rstrip("/")
        for root, _, files in os.walk(folder_path):
            rel = os.path.relpath(root, folder_path)
            # normalize: skip ".", avoid trailing "/."
            if rel in (".", "./"):
                remote_path = base
            else:
                remote_path = f"{base}/{rel.replace('\\', '/')}"

            # ensure parents exist recursively
            self._mkdir_p(sftp_client, remote_path)

            # upload files
            for fname in files:
                local_file = os.path.join(root, fname)
                sftp_client.put(local_file, f"{remote_path}/{fname}")
        return True

    def move_configuration_files(self):
        client = self.deployment_engine.get_ssh_client()
        folder_path_local = self.create_instance_file_data()

        if not client:
            raise Exception("client initialization failed")

        remote_base = f"/opt/ContainerConfiguration/{self.instance_name}"
        with client.open_sftp() as sftp:
            self._mkdir_p(sftp, remote_base)
            ok = self.upload_remote_dir(sftp, folder_path_local, remote_base)

        return {"folder_local": folder_path_local, "folder_remote": remote_base, "status": ok}

