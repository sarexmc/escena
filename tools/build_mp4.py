#!/usr/bin/env python3
"""Genera un mp4 H.264 1080p60 por escena (con audio AAC y fundidos) a partir de scenes.json.
Uso: python3 build_mp4.py video.mp4 [salida_dir]   (requiere ffmpeg)"""
import json, os, subprocess, sys
here = os.path.dirname(os.path.abspath(__file__))
video = sys.argv[1]
out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(here, "..", "videos")
os.makedirs(out, exist_ok=True)
cfg = json.load(open(os.path.join(here, "scenes.json"), encoding="utf-8"))
for sc in cfg["scenes"]:
    n, ss, dur = sc["name"], sc["start"], sc["end"] - sc["start"]
    vf = []
    if sc.get("hide_notification"):
        vf.append("delogo=x=12:y=10:w=340:h=160")       # tapa el aviso del juego
    vf += ["hqdn3d=1.2:1.2:3:3",                         # limpia ruido y artefactos
           "scale=-2:1080:flags=lanczos",                # 720p -> 1080p
           "unsharp=5:5:0.5:3:3:0.0",
           "eq=contrast=1.03:saturation=1.06",
           "fps=60",
           f"fade=t=in:st=0:d=0.6,fade=t=out:st={dur-0.6:.2f}:d=0.6"]
    subprocess.run(["ffmpeg","-y","-v","error","-ss",str(sc["start"]),"-t",str(dur),"-i",video,
        "-vf",",".join(vf),"-af",f"afade=t=in:st=0:d=0.6,afade=t=out:st={dur-0.6:.2f}:d=0.6",
        "-c:v","libx264","-preset","faster","-crf","21","-pix_fmt","yuv420p",
        "-c:a","aac","-b:a","192k","-movflags","+faststart",
        os.path.join(out, n + ".mp4")], check=True)
    print("OK", n, flush=True)
