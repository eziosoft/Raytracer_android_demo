```shell
cd AI
```

```shell
adb pull /data/data/com.example.fps_raytrace/files/app_logs.txt app_logs.txt
```

```shell
source env/bin/activate
python train.py
```

```shell
cp ai_movement.tflite ../app/src/main/assets/ai_movement.tflite
```