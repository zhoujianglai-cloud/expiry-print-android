import json
import re
from pathlib import Path


SOURCE = Path(__file__).resolve().parents[2] / "jadx-output/sources/com/fjxm/print/db/DataManager.java"
OUTPUT = Path(__file__).resolve().parents[1] / "app/src/main/assets/materials.json"


def parse_value(raw: str):
    raw = raw.strip()
    if raw.startswith('"'):
        return json.loads(raw)
    return int(raw.rstrip("L"))


def main():
    source = SOURCE.read_text(encoding="utf-8")
    records = []
    for match in re.finditer(
        r"MaterialEntity\s+(materialEntity\d*)\s*=\s*new MaterialEntity\(\);(.*?)(?=arrayList\.add\(\1\);)",
        source,
        re.S,
    ):
        variable, block = match.groups()
        item = {
            "type": 0,
            "typeName": "",
            "cateId": 0,
            "cateName": "",
            "productId": len(records) + 1,
            "product": "",
            "storeType": 1,
            "refrigerationHours": 0,
            "normalHours": 0,
            "freezingHours": 0,
            "remarks": "",
        }
        field_map = {
            "Type": "type",
            "TypeName": "typeName",
            "CateId": "cateId",
            "CateName": "cateName",
            "ProductId": "productId",
            "Product": "product",
            "StoreType": "storeType",
            "RefrigerationTime": "refrigerationHours",
            "NormalTemperatureTime": "normalHours",
            "FreezingTime": "freezingHours",
            "Remarsk": "remarks",
        }
        for setter, key in field_map.items():
            setter_match = re.search(
                rf"{re.escape(variable)}\.set{setter}\((\"(?:[^\"\\]|\\.)*\"|-?\d+L?)\);",
                block,
            )
            if setter_match:
                item[key] = parse_value(setter_match.group(1))
        if item["product"]:
            records.append(item)

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {len(records)} materials to {OUTPUT}")


if __name__ == "__main__":
    main()
