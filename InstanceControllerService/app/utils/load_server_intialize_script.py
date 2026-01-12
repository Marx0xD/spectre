from pathlib import Path

def load_server_exec_script(os_type:str)->str:
    base_dir = Path(__file__).parent.parent
    if os_type == "ubuntu":
        file_name = "intialize-ubuntu-host.sh"
    elif os_type == 'fedora':
        file_name = "intialize_fedora_host.sh"
    else:
        raise Exception("host not found")
    file_dir = base_dir/ "Scripts" / "Remote-Host-OS" / file_name
    with open(file_dir,"r") as file:
        file_content = file.read()
    return  file_content
