import tensorflow as tf
import numpy as np
import matplotlib.pyplot as plt
import argparse
import os
from tensorflow.keras import layers, regularizers
from tensorflow.keras.callbacks import EarlyStopping

# Training parameters
EPOCHS = 1000
BATCH_SIZE = 2000
LEARNING_RATE = 0.0003
INPUT_SIZE = 36 * 2 + 2   # 36 ray distances  + 36 object types + 2 player positions
OUTPUT_SIZE = 5           # Forwards, Backwards, Left, Right, Shoot
MODEL_FILE = "ai_movement.keras"
TFLITE_FILE = "ai_movement.tflite"


def create_model():
    """Creates and returns a new TensorFlow model"""
    print("Creating a new model...")
    model = tf.keras.Sequential([
        layers.Input(shape=(INPUT_SIZE,)),
        layers.Dense(256, activation='relu', kernel_regularizer=regularizers.l2(0.001)),
        layers.Dropout(0.4),
        layers.Dense(128, activation='relu', kernel_regularizer=regularizers.l2(0.001)),
        layers.Dropout(0.4),
        layers.Dense(64, activation='relu', kernel_regularizer=regularizers.l2(0.001)),
        layers.Dropout(0.4),
        layers.Dense(32, activation='relu'),
        layers.Dense(OUTPUT_SIZE, activation='sigmoid')
    ])

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=LEARNING_RATE),
        loss=tf.keras.losses.BinaryCrossentropy(label_smoothing=0.1),
        metrics=['accuracy']
    )

    return model


def train_model(model, dataset_file):
    """Trains the model on the dataset and generates training graphs"""
    print(f"Loading training data from {dataset_file}...")

    try:
        data = np.loadtxt(dataset_file, delimiter=";")
        print("Data loaded successfully!")
    except Exception as e:
        print("Error loading data:", e)
        return

    # Split data into inputs (X) and outputs (y)
    X_train = data[:, :INPUT_SIZE]
    y_train = data[:, INPUT_SIZE:INPUT_SIZE + OUTPUT_SIZE]

    print("X_train shape:", X_train.shape)
    print("y_train shape:", y_train.shape)

    # Early stopping with lower patience
    early_stopping = EarlyStopping(monitor='val_loss', patience=5, restore_best_weights=True)

    print("Training model...")
    history = model.fit(
        X_train, y_train,
        epochs=EPOCHS,
        batch_size=BATCH_SIZE,
        validation_split=0.2,
        callbacks=[early_stopping]
    )

    # Save the trained model
    model.save(MODEL_FILE)
    print(f"Model saved as {MODEL_FILE}")

    # Plot training and validation loss
    plt.figure(figsize=(10, 6))
    plt.plot(history.history['loss'], label='Training Loss')
    plt.plot(history.history['val_loss'], label='Validation Loss')
    plt.title('Training and Validation Loss over Epochs')
    plt.xlabel('Epochs')
    plt.ylabel('Loss')
    plt.legend()
    plt.grid(True)
    plt.savefig('training_loss_plot.png')
    print("Saved training loss plot as training_loss_plot.png")

    # Plot training and validation accuracy
    plt.figure(figsize=(10, 6))
    plt.plot(history.history['accuracy'], label='Training Accuracy')
    plt.plot(history.history['val_accuracy'], label='Validation Accuracy')
    plt.title('Training and Validation Accuracy over Epochs')
    plt.xlabel('Epochs')
    plt.ylabel('Accuracy')
    plt.legend()
    plt.grid(True)
    plt.savefig('training_accuracy_plot.png')
    print("Saved training accuracy plot as training_accuracy_plot.png")

    plt.show()


def convert_to_tflite():
    """Converts the trained model to TensorFlow Lite format"""
    if not os.path.exists(MODEL_FILE):
        print(f"Error: {MODEL_FILE} not found. Train or create a model first.")
        return

    print("Converting model to TensorFlow Lite...")
    model = tf.keras.models.load_model(MODEL_FILE)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()

    with open(TFLITE_FILE, "wb") as f:
        f.write(tflite_model)

    print(f"TensorFlow Lite model saved as {TFLITE_FILE}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Train and manage a TensorFlow model.")
    parser.add_argument("action", choices=["create", "train", "convert"], help="Action to perform")
    parser.add_argument("--dataset", type=str, help="Dataset file for training (Required for 'train')")

    args = parser.parse_args()

    if args.action == "create":
        model = create_model()
        model.save(MODEL_FILE)
        print(f"New model created and saved as {MODEL_FILE}")

    elif args.action == "train":
        if not args.dataset:
            print("Error: --dataset argument is required for training.")
        else:
            if os.path.exists(MODEL_FILE):
                print(f"Loading existing model from {MODEL_FILE}...")
                model = tf.keras.models.load_model(MODEL_FILE)
            else:
                print("No existing model found. Creating a new one...")
                model = create_model()
            train_model(model, args.dataset)

    elif args.action == "convert":
        convert_to_tflite()
