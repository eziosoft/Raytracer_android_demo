import tensorflow as tf
import numpy as np
import matplotlib.pyplot as plt
from tensorflow.keras import layers
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau

# Training parameters
epochs = 2000
batch_size = 512  # 🔥 Increased for stability

# Input/Output sizes
input_size = 36 * 2
output_size = 3

print("Creating optimized model...")

# 🚀 IMPROVED ARCHITECTURE
model = tf.keras.Sequential([
    layers.Input(shape=(input_size,)),

    layers.Dense(512, activation='swish', kernel_regularizer=tf.keras.regularizers.l2(0.00005)),
    layers.BatchNormalization(),
    layers.Dropout(0.1),

    layers.Dense(256, activation='swish', kernel_regularizer=tf.keras.regularizers.l2(0.00005)),
    layers.BatchNormalization(),
    layers.Dropout(0.1),

    layers.Dense(128, activation='swish'),

    layers.Dense(64, activation='swish'),

    layers.Dense(output_size, activation='tanh')
])

# 🚀 IMPROVED LOSS & OPTIMIZER
model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=1e-4),
              loss=tf.keras.losses.Huber(delta=1.0),
              metrics=['mae'])

print("Loading training data...")
try:
    data = np.loadtxt("app_logs.txt", delimiter=";")
    print("Data loaded successfully!")
except Exception as e:
    print("Error loading data:", e)
    exit()

# 🔄 NORMALIZATION & NOISE INJECTION
X_train = data[:, :input_size]
y_train = data[:, input_size:input_size + output_size]

X_mean = np.mean(X_train, axis=0)
X_std = np.std(X_train, axis=0) + 1e-8
X_train = (X_train - X_mean) / X_std

# 🔥 Add noise to prevent overfitting
X_train += np.random.normal(0, 0.01, X_train.shape)

print("Training shape:", X_train.shape, y_train.shape)

# 📉 CALLBACKS
early_stopping = EarlyStopping(monitor='val_loss', patience=50, restore_best_weights=True)
lr_scheduler = ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=10, min_lr=1e-6)

# 🚀 TRAIN MODEL
print("Training model with final tweaks...")
history = model.fit(
    X_train, y_train,
    epochs=epochs,
    batch_size=batch_size,
    validation_split=0.3,
    callbacks=[early_stopping, lr_scheduler]
)

print("Converting to TensorFlow Lite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

with open("ai_movement.tflite", "wb") as f:
    f.write(tflite_model)

print("Model trained and saved as ai_movement.tflite")
print("Done!")
