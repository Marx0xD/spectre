def parse_os_release(content: str) -> dict[str, str]:
    result = {}

    for line in content.splitlines():
        line = line.strip()

        # skip empty lines and comments
        if not line or line.startswith("#"):
            continue

        if "=" not in line:
            continue

        key, value = line.split("=", 1)

        # strip surrounding quotes if present
        value = value.strip().strip('"')

        result[key] = value

    return result
