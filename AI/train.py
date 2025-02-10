import tensorflow as tf
import numpy as np
import matplotlib.pyplot as plt
from tensorflow.keras import layers
from tensorflow.keras.callbacks import EarlyStopping

# Define training parameters
epochs = 2000
batch_size = 128  # Increased batch size for smoother training

# Define input and output sizes
# Input is an array of 36 ray distances + 36 object types + 2 player X Y positions
input_size = 36 * 2 + 2   # 36 ray distances  + 36 object types + 2 player positions
output_size = 5  # 4 movement keys + 1 shoot button, Forwards, Backwards, Left, Right, Shoot

print("Training model for enemy AI movement...")
print("Input size:", input_size)
print("Output size:", output_size)
print("-----------------------------")

print("Creating model...")
# Define the model with dropout and L2 regularization to prevent overfitting
model = tf.keras.Sequential([
    layers.Input(shape=(input_size,)),
    layers.Dense(512, activation='relu', kernel_regularizer=tf.keras.regularizers.l2(0.001)),
    layers.Dropout(0.2),
    layers.Dense(256, activation='relu', kernel_regularizer=tf.keras.regularizers.l2(0.001)),
    layers.Dropout(0.2),
    layers.Dense(128, activation='relu', kernel_regularizer=tf.keras.regularizers.l2(0.001)),
    layers.Dropout(0.3),
    layers.Dense(64, activation='relu'),
    layers.Dense(output_size, activation='sigmoid')
])

# Compile the model
model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=0.0005), loss='binary_crossentropy', metrics=['accuracy'])

print("Loading training data...")
try:
    data = np.loadtxt("app_logs.txt", delimiter=";")
    print("Data loaded successfully!")
except Exception as e:
    print("Error loading data:", e)
    exit()

print("First row of data:", data[0])

# Split data into inputs (X) and outputs (y)
X_train = data[:, :input_size]
y_train = data[:, input_size:input_size + output_size]  # Explicit slicing for safety

print("X_train shape:", X_train.shape)
print("y_train shape:", y_train.shape)

print("Training data loaded...")
print("Data size:", data.shape)
input("Press Enter to continue...")

# --- Custom Callback for Real-Time Plot ---
class LivePlotCallback(tf.keras.callbacks.Callback):
    def __init__(self):
        super().__init__()
        self.losses = []
        self.val_losses = []

        plt.ion()  # Interactive mode
        self.fig, self.ax = plt.subplots()
        self.line1, = self.ax.plot([], [], label='Training Loss', color='blue')
        self.line2, = self.ax.plot([], [], label='Validation Loss', color='orange')
        self.ax.set_title('Training Progress')
        self.ax.set_xlabel('Epochs')
        self.ax.set_ylabel('Loss')
        self.ax.legend()
        self.ax.grid(True)

        plt.show(block=False)  # Non-blocking show

    def on_epoch_end(self, epoch, logs=None):
        logs = logs or {}
        self.losses.append(logs.get('loss', 0.0))
        self.val_losses.append(logs.get('val_loss', 0.0))  # Prevents key errors

        # Update plot data
        self.line1.set_xdata(range(len(self.losses)))
        self.line1.set_ydata(self.losses)
        self.line2.set_xdata(range(len(self.val_losses)))
        self.line2.set_ydata(self.val_losses)

        self.ax.relim()
        self.ax.autoscale_view()

        plt.draw()
        plt.pause(0.01)  # Update the figure

# Add early stopping to prevent overfitting
early_stopping = EarlyStopping(monitor='val_loss', patience=20, restore_best_weights=True)

print("Training model...")
# Train the model with real-time loss plot and early stopping
history = model.fit(
    X_train, y_train,
    epochs=epochs,
    batch_size=batch_size,
    validation_split=0.2,
    callbacks=[LivePlotCallback(), early_stopping]
)

print("Converting to TensorFlow Lite...")
# Convert to TensorFlow Lite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Save for Android
with open("ai_movement.tflite", "wb") as f:
    f.write(tflite_model)

print("Model trained and saved as ai_movement.tflite")
print("Done!")
input("Press Enter to exit...")