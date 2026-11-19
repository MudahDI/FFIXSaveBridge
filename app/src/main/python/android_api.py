"""Stable file-based API called by the Android UI."""
import json
import os
import time
import zipfile
from pathlib import Path

from ffix_conv import convert, model, pc_container, ps1_card, ps1_save


def inspect_file(path):
    raw = Path(path).read_bytes()
    container_name, entries = ps1_card.parse_file(raw)
    result = []
    for entry in entries:
        try:
            save = ps1_save.PS1Save(entry.data, entry.product_code, entry.filename)
            region, language, disc = save.region
            preview = save.preview()
            result.append({
                "block": entry.block_index,
                "filename": entry.filename,
                "product_code": entry.product_code,
                "region": region,
                "disc_hint": disc,
                "crc_ok": save.crc_ok,
                "crc_stored": save.crc_stored,
                "crc_computed": save.crc_computed,
                "leader": preview["leader_name"],
                "level": preview["leader_level"],
                "location": preview["location"],
                "gil": preview["gil"],
                "playtime_seconds": round(preview["playtime_frames"] / (50 if region == "PAL" else 60), 1),
            })
        except Exception as exc:
            result.append({
                "block": entry.block_index,
                "filename": entry.filename,
                "product_code": entry.product_code,
                "error": str(exc),
            })
    return json.dumps({"container": container_name, "entries": result})


def convert_file(input_path, output_path):
    raw = Path(input_path).read_bytes()
    container_name, entries = ps1_card.parse_file(raw)
    if not entries:
        raise ValueError("Tidak ada save Final Fantasy IX pada file ini.")
    if len(entries) > pc_container.SAVE_COUNT:
        raise ValueError("Kartu berisi lebih dari 15 save; batch ini belum bisa ditempatkan dalam satu slot PC.")

    output = convert.blank_container()
    reports = []
    converted = 0
    timestamp = time.time()

    for file_index, entry in enumerate(entries):
        save = ps1_save.PS1Save(entry.data, entry.product_code, entry.filename)
        canonical = model.from_ps1(save)
        tree, report = convert.to_pc_tree(canonical, strict=True)
        preview = convert.preview_for(tree, canonical, timestamp + file_index)
        convert.insert(output, 0, file_index, tree, preview, overwrite=False)
        reports.append({
            "source_block": entry.block_index,
            "source_name": entry.filename,
            "region": canonical["source"]["region"],
            "location": canonical["preview"]["location"],
            "scenario": canonical["event"]["scenario_counter"],
            "mapped": report.mapped,
            "transformed": report.transformed,
            "defaulted": report.defaulted,
            "dropped_or_unmapped": report.dropped,
            "warnings": report.warnings,
        })
        converted += 1

    # Independently reopen the generated container and verify every encrypted slot.
    verified = pc_container.Container(output.to_bytes())
    if len(verified.occupied()) != converted:
        raise ValueError("Verifikasi gagal: jumlah slot hasil tidak sesuai.")
    for file_index in range(converted):
        if verified.slot_tree(0, file_index) is None:
            raise ValueError("Verifikasi gagal: slot hasil tidak dapat dibaca ulang.")

    Path(output_path).parent.mkdir(parents=True, exist_ok=True)
    temp_path = str(output_path) + ".tmp"
    with zipfile.ZipFile(temp_path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("SavedData_ww.dat", verified.to_bytes())
    os.replace(temp_path, output_path)

    return json.dumps({
        "success": True,
        "converted": converted,
        "target_slot": 1,
        "files": [item["source_name"] for item in reports],
        "reports": reports,
        "verification": "Container metadata and all encrypted save slots were reopened successfully.",
    })
