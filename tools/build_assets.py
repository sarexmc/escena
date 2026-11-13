#!/usr/bin/env python3
"""Convierte los mp4 de una carpeta en los assets que lleva DENTRO del mod (fotogramas + audio).
Uso: python3 build_assets.py carpeta_con_mp4   (requiere ffmpeg)"""
import json, os, subprocess, sys, glob
here = os.path.dirname(os.path.abspath(__file__))
src = sys.argv[1]
res = os.path.join(here, "..", "src", "main", "resources", "assets", "sarexstudio")
vd, sd = os.path.join(res, "cinematics"), os.path.join(res, "sounds")
os.makedirs(vd, exist_ok=True); os.makedirs(sd, exist_ok=True)
cfg = json.load(open(os.path.join(here, "scenes.json"), encoding="utf-8"))
W, H, FPS, Q = 1280, 720, 24, 9
sounds, meta = {}, {}
for sc in cfg["scenes"]:
    n = sc["name"]; f = os.path.join(src, n + ".mp4")
    dur = float(subprocess.check_output(["ffprobe","-v","error","-show_entries","format=duration","-of","csv=p=0",f]))
    subprocess.run(["ffmpeg","-y","-v","error","-i",f,"-an","-vf",f"scale={W}:{H}:flags=lanczos,fps={FPS}",
        "-c:v","mjpeg","-q:v",str(Q),"-pix_fmt","yuvj420p","-f","mjpeg",os.path.join(vd,n+".mjpg")],check=True)
    subprocess.run(["ffmpeg","-y","-v","error","-i",f,"-vn","-c:a","libvorbis","-q:a","4","-ac","2","-ar","44100",
        os.path.join(sd,n+".ogg")],check=True)
    sounds[n] = {"sounds": [{"name": "sarexstudio:"+n, "stream": True}]}
    meta[n] = {"fps": FPS, "width": W, "height": H, "duration": round(dur,3)}
    print("OK", n, flush=True)
json.dump(sounds, open(os.path.join(res,"sounds.json"),"w"), indent=2)
json.dump(meta, open(os.path.join(vd,"index.json"),"w"), indent=2)
