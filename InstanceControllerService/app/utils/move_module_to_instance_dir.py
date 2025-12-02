import os
import shutil


class ModuleManagement:
    @staticmethod
    def move_module_to_instance_dir(self,instance_path, module_dir):
        try:
            # Check if the destination directory already exists
            dest = os.path.join(instance_path, os.path.basename(module_dir))  # Destination path

            # If it exists, remove or rename it (depending on your use case)
            if os.path.exists(dest):
                print(f"Directory '{dest}' already exists. Attempting to delete...")
                shutil.rmtree(dest)  # Delete the existing directory if you want to overwrite
                print(f"Deleted existing directory '{dest}'")

            # Now copy the module to the destination
            shutil.copytree(module_dir, dest)
            print(f"Module successfully copied to {dest}")
            return True

        except Exception as e:
            print(f"Error while moving module: {e}")
            return False


