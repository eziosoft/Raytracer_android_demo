import tensorflow as tf
import numpy as np
import matplotlib.pyplot as plt
from tensorflow.keras import layers, regularizers
from tensorflow.keras.callbacks import EarlyStopping

# Define training parameters
epochs = 1000
batch_size = 2000

# Define input and output sizes
input_size = 36 * 2 + 2   # 36 ray distances  + 36 object types + 2 player positions
output_size = 5  # Forwards, Backwards, Left, Right, Shoot

print("Creating optimized model...")
model = tf.keras.Sequential([
    layers.Input(shape=(input_size,)),
    layers.Dense(256, activation='relu', kernel_regularizer=regularizers.l2(0.001)),
    layers.Dropout(0.4),
    layers.Dense(128, activation='relu', kernel_regularizer=regularizers.l2(0.001)),
    layers.Dropout(0.4),
    layers.Dense(64, activation='relu', kernel_regularizer=regularizers.l2(0.001)),
    layers.Dropout(0.4),
    layers.Dense(32, activation='relu'),
    layers.Dense(output_size, activation='sigmoid')
])

# Compile the model with a lower learning rate and label smoothing
model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=0.0003),
    loss=tf.keras.losses.BinaryCrossentropy(label_smoothing=0.1),
    metrics=['accuracy']
)

print("Loading training data...")
try:
    data = np.loadtxt("app_logs.txt", delimiter=";")
    print("Data loaded successfully!")
except Exception as e:
    print("Error loading data:", e)
    exit()

# Split data into inputs (X) and outputs (y)
X_train = data[:, :input_size]
y_train = data[:, input_size:input_size + output_size]

print("X_train shape:", X_train.shape)
print("y_train shape:", y_train.shape)

# Early stopping with lower patience
early_stopping = EarlyStopping(monitor='val_loss', patience=5, restore_best_weights=True)

print("Training model...")
history = model.fit(
    X_train, y_train,
    epochs=epochs,
    batch_size=batch_size,
    validation_split=0.2,
    callbacks=[early_stopping]
)

# Converting to TensorFlow Lite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

with open("ai_movement.tflite", "wb") as f:
    f.write(tflite_model)

print("Optimized model trained and saved as ai_movement_optimized.tflite")

# Plotting training and validation loss
plt.figure(figsize=(10, 6))
plt.plot(history.history['loss'], label='Training Loss')
plt.plot(history.history['val_loss'], label='Validation Loss')
plt.title('Training and Validation Loss over Epochs')
plt.xlabel('Epochs')
plt.ylabel('Loss')
plt.legend()
plt.grid(True)

# Save the plot as a PNG file
plt.savefig('training_loss_plot.png')

# Also, plot training and validation accuracy
plt.figure(figsize=(10, 6))
plt.plot(history.history['accuracy'], label='Training Accuracy')
plt.plot(history.history['val_accuracy'], label='Validation Accuracy')
plt.title('Training and Validation Accuracy over Epochs')
plt.xlabel('Epochs')
plt.ylabel('Accuracy')
plt.legend()
plt.grid(True)

# Save the plot as a PNG file
plt.savefig('training_accuracy_plot.png')

plt.show()
