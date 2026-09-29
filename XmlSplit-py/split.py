from lxml import etree
from pathlib import Path

INPUT_FILE = "D:/temp/FAO-IATI/IATI-2020-2026.xml"
OUTPUT_DIR = Path("D:/temp/FAO-IATI/split")
ACTIVITIES_PER_FILE = 700

OUTPUT_DIR.mkdir(exist_ok=True)


def open_output_file(file_number, root_attributes):
    filename = OUTPUT_DIR / f"iati_activities_{file_number:04d}.xml"

    f = open(filename, "wb")

    # XML declaration
    f.write(b'<?xml version="1.0" encoding="UTF-8"?>\n')

    # Recreate root element and preserve its attributes
    root_start = "<iati-activities"

    for key, value in root_attributes.items():
        escaped_value = (
            value.replace("&", "&amp;")
                 .replace('"', "&quot;")
                 .replace("<", "&lt;")
                 .replace(">", "&gt;")
        )

        root_start += f' {key}="{escaped_value}"'

    root_start += ">\n"

    f.write(root_start.encode("utf-8"))

    return f, filename


def close_output_file(f):
    f.write(b"</iati-activities>\n")
    f.close()


context = etree.iterparse(
    INPUT_FILE,
    events=("start", "end"),
    huge_tree=True
)

activity_count = 0
file_number = 0
output_file = None
root_attributes = {}

for event, elem in context:

    # First element = root
    if event == "start" and elem.tag == "iati-activities":
        root_attributes = dict(elem.attrib)

    if event == "end" and elem.tag == "iati-activity":

        # Start a new output file when required
        if activity_count % ACTIVITIES_PER_FILE == 0:

            if output_file is not None:
                close_output_file(output_file)

            file_number += 1

            output_file, filename = open_output_file(
                file_number,
                root_attributes
            )

            print(f"Writing {filename}")

        # Serialize complete iati-activity subtree
        output_file.write(
            etree.tostring(
                elem,
                encoding="UTF-8",
                pretty_print=True
            )
        )

        output_file.write(b"\n")

        activity_count += 1

        # Very important:
        # release memory occupied by this activity
        elem.clear()

        while elem.getprevious() is not None:
            del elem.getparent()[0]


if output_file is not None:
    close_output_file(output_file)

print(f"Processed {activity_count:,} activities")
print(f"Created {file_number} files")