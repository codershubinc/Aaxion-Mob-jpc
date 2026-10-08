with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/AaxionDriveScreen.kt", "r") as f:
    content = f.read()

sidebar_start = "                // Collapsible Sidebar with rounded corners (Overlaid with smooth slide, 0 frame drops)"
sidebar_end = "            }\n        } else {"

start_idx = content.find(sidebar_start)
end_idx = content.find(sidebar_end, start_idx)

if start_idx != -1 and end_idx != -1:
    sidebar_block = content[start_idx:end_idx]
    
    # Remove from original location
    content = content[:start_idx] + content[end_idx:]
    
    # Place it at the end of the main Box
    last_brace = content.rfind("        }\n    }\n}")
    
    if last_brace != -1:
        # adjust indent
        new_sidebar_block = "\n".join("    " + line[8:] if line.startswith("        ") else line for line in sidebar_block.split("\n"))
        content = content[:last_brace] + "        }\n" + sidebar_block + "    }\n}" + content[last_brace+14:]
    else:
        print("Couldn't find insertion point")
    
    with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/AaxionDriveScreen.kt", "w") as f:
        f.write(content)
else:
    print("Couldn't find sidebar block")
