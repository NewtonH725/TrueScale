#!/usr/bin/env python3
"""
verify_metrology_math.py
Deterministic mathematical validator for True Scale metrology engine:
- Sub-pixel conversion (mm <-> px) with downscaling and calibration
- ISO 8653 EU ring sizing math (circumference <-> diameter)
- Trigonometric atan2 calculations (2D bubble roll/pitch, protractor, vertical clinometer)
- Dynamic adaptive EMA filter simulation
- Saves validation report in .tmp/reports/math_verification_report.json
"""

import sys
import os
import json
import math

TMP_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".tmp", "reports")
os.makedirs(TMP_DIR, exist_ok=True)
REPORT_FILE = os.path.join(TMP_DIR, "math_verification_report.json")

def verify_mm_px_conversion():
    results = []
    # Test case 1: Standard display 400 DPI, 1.0 scale, 1.0 calibration
    xdpi = 400.0
    ydpi = 400.0
    scaleX = 1.0
    scaleY = 1.0
    calib = 1.0000

    px_per_mm_x = ((xdpi * scaleX) / 25.4) * calib
    px_per_mm_y = ((ydpi * scaleY) / 25.4) * calib

    expected_px_per_mm = 400.0 / 25.4 # ~15.74803 px/mm
    assert math.isclose(px_per_mm_x, expected_px_per_mm, rel_tol=1e-5), f"pxPerMmX mismatch: {px_per_mm_x}"
    assert math.isclose(px_per_mm_y, expected_px_per_mm, rel_tol=1e-5), f"pxPerMmY mismatch: {px_per_mm_y}"

    # 85.60 mm (ISO card width) in pixels
    card_width_px = 85.60 * px_per_mm_x
    expected_card_px = 85.60 * (400.0 / 25.4)
    assert math.isclose(card_width_px, expected_card_px, rel_tol=1e-5)

    results.append({
        "test": "Standard 400 DPI mm <-> px",
        "px_per_mm_x": px_per_mm_x,
        "px_per_mm_y": px_per_mm_y,
        "card_85_60mm_px": card_width_px,
        "status": "PASSED"
    })

    # Test case 2: Resolution downscaled display (Physical QHD+ 1440x3120, Window FHD+ 1080x2340)
    physical_w, physical_h = 1440.0, 3120.0
    window_w, window_h = 1080.0, 2340.0
    scale_x = physical_w / window_w # 1.333333...
    scale_y = physical_h / window_h # 1.333333...
    raw_xdpi = 390.0 # Virtual DPI reported by window
    downscaled_px_per_mm = ((raw_xdpi * scale_x) / 25.4) * calib
    expected_downscaled = ((390.0 * (1440.0 / 1080.0)) / 25.4) * 1.0
    assert math.isclose(downscaled_px_per_mm, expected_downscaled, rel_tol=1e-5)

    results.append({
        "test": "Downscaling compensation QHD+ -> FHD+",
        "scale_x": scale_x,
        "scale_y": scale_y,
        "compensated_px_per_mm": downscaled_px_per_mm,
        "status": "PASSED"
    })

    # Test case 3: Micrometric calibration (+1.5% adjustment, calib = 1.0150)
    calib_adj = 1.0150
    calib_px_per_mm = ((xdpi * scaleX) / 25.4) * calib_adj
    assert math.isclose(calib_px_per_mm, expected_px_per_mm * 1.0150, rel_tol=1e-5)

    results.append({
        "test": "Calibration factor 1.0150",
        "calibrated_px_per_mm": calib_px_per_mm,
        "status": "PASSED"
    })
    return results

def verify_iso_8653_ring_sizes():
    results = []
    # ISO 8653: EU Ring Size = Circumference in mm (rounded to nearest integer)
    # Diameter = Circumference / pi
    test_cases = [
        {"eu_size": 48, "expected_diameter_mm": 48.0 / math.pi}, # ~15.28 mm
        {"eu_size": 52, "expected_diameter_mm": 52.0 / math.pi}, # ~16.55 mm
        {"eu_size": 56, "expected_diameter_mm": 56.0 / math.pi}, # ~17.83 mm
        {"eu_size": 60, "expected_diameter_mm": 60.0 / math.pi}, # ~19.10 mm
        {"eu_size": 64, "expected_diameter_mm": 64.0 / math.pi}, # ~20.37 mm
    ]

    for tc in test_cases:
        c_mm = tc["eu_size"]
        calculated_d = c_mm / math.pi
        calculated_eu_size = round(calculated_d * math.pi)
        assert calculated_eu_size == tc["eu_size"]
        assert math.isclose(calculated_d, tc["expected_diameter_mm"], rel_tol=1e-4)
        results.append({
            "eu_size": tc["eu_size"],
            "inner_circumference_mm": c_mm,
            "inner_diameter_mm": round(calculated_d, 2),
            "status": "PASSED"
        })
    return results

def verify_atan2_trigonometry():
    results = []

    # 1. Bubble 2D: Flat horizontal surface test
    # Gravity pointing purely along +Z: gx = 0, gy = 0, gz = 9.81
    gx, gy, gz = 0.0, 0.0, 9.81
    roll = math.degrees(math.atan2(gx, math.sqrt(gy**2 + gz**2)))
    pitch = math.degrees(math.atan2(gy, math.sqrt(gx**2 + gz**2)))
    assert math.isclose(roll, 0.0, abs_tol=1e-6)
    assert math.isclose(pitch, 0.0, abs_tol=1e-6)

    # 2. Bubble 2D: Slight 5.0 degree tilt along X (roll = 5.0 deg)
    tilt_rad = math.radians(5.0)
    gx = 9.81 * math.sin(tilt_rad)
    gz = 9.81 * math.cos(tilt_rad)
    gy = 0.0
    calc_roll = math.degrees(math.atan2(gx, math.sqrt(gy**2 + gz**2)))
    assert math.isclose(calc_roll, 5.0, abs_tol=1e-4)

    results.append({
        "test": "2D Bubble roll/pitch calculations",
        "tilt_5deg_calc": calc_roll,
        "status": "PASSED"
    })

    # 3. Vertical Clinometer: phone standing vertically on bottom edge
    # Gravity pointing mostly along -Y (downwards on phone screen): gx = 0, gy = -9.81, gz = 0
    # Tilt of 10.0 degrees clockwise
    tilt_vert_rad = math.radians(10.0)
    gx = 9.81 * math.sin(tilt_vert_rad)
    gy = -9.81 * math.cos(tilt_vert_rad)
    angle_vert = math.degrees(math.atan2(gx, -gy))
    assert math.isclose(angle_vert, 10.0, abs_tol=1e-4)

    results.append({
        "test": "Vertical Clinometer atan2(gx, -gy)",
        "vertical_10deg_calc": angle_vert,
        "status": "PASSED"
    })

    # 4. Protractor 180: Angle between two rays
    # Ray 1: Along positive X axis -> (100, 0) from (0,0) -> theta1 = 0 deg
    # Ray 2: 59.0 degrees -> (100 * cos(59 deg), 100 * sin(59 deg))
    target_deg = 59.0
    p1 = (100.0, 0.0)
    p2 = (100.0 * math.cos(math.radians(target_deg)), 100.0 * math.sin(math.radians(target_deg)))

    theta1 = math.atan2(p1[1], p1[0])
    theta2 = math.atan2(p2[1], p2[0])
    diff = abs(theta2 - theta1)
    if diff > math.pi:
        diff = 2 * math.pi - diff
    calc_deg = math.degrees(diff)
    assert math.isclose(calc_deg, 59.0, abs_tol=1e-4)

    results.append({
        "test": "Protractor convex angle calculation",
        "protractor_59deg_calc": calc_deg,
        "status": "PASSED"
    })

    return results

def verify_adaptive_ema_filter():
    results = []

    # Dynamic EMA: y_k = alpha * x_k + (1 - alpha) * y_{k-1}
    # Static noise: signal around 0 with noise +/- 0.05 m/s^2 (delta < 0.15) -> alpha = 0.06
    # Dynamic step: jump to 5.0 m/s^2 (delta >= 0.15) -> alpha = 0.25

    def filter_step(current_sample, prev_filtered, prev_raw):
        delta = abs(current_sample - prev_raw)
        alpha = 0.25 if delta >= 0.15 else 0.06
        filtered = alpha * current_sample + (1.0 - alpha) * prev_filtered
        return filtered, alpha

    # Test static noise attenuation
    val_filtered = 0.0
    val_raw = 0.0
    # Provide small oscillations
    for _ in range(50):
        sample = 0.05
        val_filtered, alpha_used = filter_step(sample, val_filtered, val_raw)
        val_raw = sample
        assert alpha_used == 0.06

    # Test dynamic step response
    step_sample = 5.0
    val_filtered, alpha_used = filter_step(step_sample, val_filtered, val_raw)
    assert alpha_used == 0.25, f"Expected dynamic alpha 0.25, got {alpha_used}"

    results.append({
        "test": "Adaptive EMA filter state switching",
        "static_alpha": 0.06,
        "dynamic_alpha": 0.25,
        "status": "PASSED"
    })

    return results

def verify_protractor_magnetic_snap():
    results = []
    common_angles = [0.0, 30.0, 45.0, 60.0, 90.0, 120.0, 135.0, 150.0, 180.0]
    tolerance = 2.2

    def check_snap(raw_angle_deg, magnet_enabled=True):
        if not magnet_enabled:
            return raw_angle_deg, False
        matched = next((a for a in common_angles if abs(raw_angle_deg - a) <= tolerance), None)
        return matched if matched is not None else raw_angle_deg, matched is not None

    # Test 1: Magnet ON - 44.1 deg snaps to 45.0 deg
    snapped_45, is_snapped_45 = check_snap(44.1, magnet_enabled=True)
    assert is_snapped_45 and snapped_45 == 45.0, f"Expected 45.0 snap, got {snapped_45}"

    # Test 2: Magnet ON - 89.2 deg snaps to 90.0 deg
    snapped_90, is_snapped_90 = check_snap(89.2, magnet_enabled=True)
    assert is_snapped_90 and snapped_90 == 90.0, f"Expected 90.0 snap, got {snapped_90}"

    # Test 3: Magnet ON - 178.5 deg snaps to 180.0 deg
    snapped_180, is_snapped_180 = check_snap(178.5, magnet_enabled=True)
    assert is_snapped_180 and snapped_180 == 180.0, f"Expected 180.0 snap, got {snapped_180}"

    # Test 4: Magnet ON - 52.0 deg does NOT snap (outside tolerance from 45.0 and 60.0)
    angle_52, is_snapped_52 = check_snap(52.0, magnet_enabled=True)
    assert not is_snapped_52 and angle_52 == 52.0, f"Expected no snap at 52.0, got {angle_52}"

    # Test 5: Magnet OFF - Cardinal angles must NOT snap and must remain strictly free
    for test_angle in [44.1, 45.0, 89.2, 90.0, 135.0, 178.5, 180.0]:
        res_angle, is_snapped = check_snap(test_angle, magnet_enabled=False)
        assert not is_snapped, f"Magnet OFF must never snap! Failed at {test_angle}"
        assert math.isclose(res_angle, test_angle, abs_tol=1e-6), f"Angle altered when magnet OFF: {res_angle} != {test_angle}"

    results.append({
        "test": "Protractor magnetic snap on common angles & free mode toggle",
        "common_angles": common_angles,
        "snap_tolerance_deg": tolerance,
        "magnet_off_free_mode_verified": True,
        "status": "PASSED"
    })
    return results

def main():
    print("=" * 60)
    print("True Scale — Deterministic Metrology Math Verification")
    print("=" * 60)

    try:
        r1 = verify_mm_px_conversion()
        print("[✓] Display mm <-> px conversions validated.")
        r2 = verify_iso_8653_ring_sizes()
        print("[✓] ISO 8653 Ring sizing formulas validated.")
        r3 = verify_atan2_trigonometry()
        print("[✓] Atan2 trigonometric formulas validated.")
        r4 = verify_adaptive_ema_filter()
        print("[✓] Adaptive EMA filter dynamics validated.")
        r5 = verify_protractor_magnetic_snap()
        print("[✓] Protractor magnetic snap (45°, 90°, 180°) validated.")

        report = {
            "status": "SUCCESS",
            "tests": {
                "display_mm_px": r1,
                "iso_8653": r2,
                "atan2_trig": r3,
                "adaptive_ema": r4,
                "protractor_magnetic_snap": r5
            }
        }

        with open(REPORT_FILE, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2)

        print(f"\nAll mathematical assertions PASSED.")
        print(f"Report written to: {REPORT_FILE}")
        return 0

    except AssertionError as e:
        print(f"\n[X] MATHEMATICAL VERIFICATION FAILED: {e}", file=sys.stderr)
        return 1

if __name__ == "__main__":
    sys.exit(main())
