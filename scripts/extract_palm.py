import json

transcript_path = r"C:\Users\ADMIN\.gemini\antigravity\brain\3dd67974-6676-434e-8080-dbae7662d6dc\.system_generated\logs\transcript_full.jsonl"
with open(transcript_path, "r", encoding="utf-8") as f:
    for line in f:
        data = json.loads(line)
        if data.get("type") == "USER_INPUT":
            content = data.get("content", "")
            if "palm-tree" in content:
                idx = content.find("{")
                last_idx = content.rfind("}")
                if idx != -1 and last_idx != -1:
                    json_str = content[idx:last_idx+1]
                    # verify it parses as json
                    parsed = json.loads(json_str)
                    print(f"Successfully parsed palm tree JSON! XSize={parsed.get('XSize')}, YSize={parsed.get('YSize')}, Arrows={len(parsed.get('Arrows', []))}")
                    with open("user_palm_tree.json", "w", encoding="utf-8") as out:
                        json.dump(parsed, out, indent=2)
                    print("Saved user_palm_tree.json")
                    break
