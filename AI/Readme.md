# enter the AI directory

```shell
cd AI
```

# pull the logs from the app

```shell
adb pull /data/data/com.example.fps_raytrace/files/app_logs.txt app_logs.txt
```

# Create a virtual environment

```shell
source env/bin/activate
```

# Create model

```shell
python train.py create
```

# Train model

```shell
python train.py train --dataset app_logs.txt
```

# Convert model to tflite

```shell
python train.py convert
```

# Copy the trained model to the app

```shell
cp ai_movement.tflite ../app/src/main/assets/ai_movement.tflite
```