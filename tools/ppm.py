from PIL import Image
import sys

def convert_png_to_ppm(png_filepath, new_size=None):
    """
    Converts a PNG image file to a plain-text PPM file (P3 format) with optional resizing.

    Args:
        png_filepath (str): The path to the input PNG file.
        new_size (tuple, optional): A tuple (width, height) to resize the image to.
                                   Defaults to None, in which case the original size is used.
    """
    try:
        # Open the PNG image file.
        img = Image.open(png_filepath)

        # Resize the image if a new size is provided.
        if new_size:
            img = img.resize(new_size, Image.LANCZOS)

        # Ensure the image is in RGB format.
        img = img.convert("RGB")

        # Get the width and height of the image.
        width, height = img.size

        # Create the output PPM filename.
        ppm_filepath = png_filepath.rsplit('.', 1)[0] + '.ppm'

        with open(ppm_filepath, 'w') as f:
            # Write the PPM header.
            # P3 signifies a plain-text PPM file.
            # The next two numbers are the width and height.
            # The last number is the maximum color value (255).
            f.write("P3\n")
            f.write(f"{width} {height}\n")
            f.write("255\n")

            # Write the pixel data.
            # Iterate through each pixel and write its RGB values.
            for y in range(height):
                for x in range(width):
                    r, g, b = img.getpixel((x, y))
                    f.write(f"{r} {g} {b} ")
                f.write("\n")

        print(f"Successfully converted {png_filepath} to {ppm_filepath}")

    except FileNotFoundError:
        print(f"Error: The file '{png_filepath}' was not found.")
    except Exception as e:
        print(f"An error occurred: {e}")

def convert_ppm_to_png(ppm_filepath, new_size=None):
    """
    Converts a PPM image file to a PNG file with optional resizing.

    Args:
        ppm_filepath (str): The path to the input PPM file.
        new_size (tuple, optional): A tuple (width, height) to resize the image to.
                                   Defaults to None, in which case the original size is used.
    """
    try:
        # Open the PPM image file.
        img = Image.open(ppm_filepath)

        # Resize the image if a new size is provided.
        if new_size:
            img = img.resize(new_size, Image.LANCZOS)

        # Ensure the image is in RGB format.
        img = img.convert("RGB")

        # Create the output PNG filename.
        png_filepath = ppm_filepath.rsplit('.', 1)[0] + '.png'

        # Save as PNG
        img.save(png_filepath, "PNG")

        print(f"Successfully converted {ppm_filepath} to {png_filepath}")

    except FileNotFoundError:
        print(f"Error: The file '{ppm_filepath}' was not found.")
    except Exception as e:
        print(f"An error occurred: {e}")

# This part of the script allows it to be run from the command line.
# It checks for arguments and determines conversion direction based on file extension.
if __name__ == "__main__":
    if len(sys.argv) < 2 or len(sys.argv) > 4:
        print("Usage: python ppm.py [width] [height] <input_file.png|ppm>")
        print("  Converts PNG to PPM or PPM to PNG based on input file extension")
        print("  Optional width and height parameters for resizing")
    else:
        file_path = sys.argv[-1]  # Get the last argument as the file path

        # Determine conversion direction based on file extension
        if file_path.lower().endswith('.png'):
            # PNG to PPM conversion
            if len(sys.argv) == 4:
                try:
                    new_width = int(sys.argv[1])
                    new_height = int(sys.argv[2])
                    convert_png_to_ppm(file_path, new_size=(new_width, new_height))
                except ValueError:
                    print("Error: Width and height must be integers.")
            else:
                convert_png_to_ppm(file_path)
        elif file_path.lower().endswith('.ppm'):
            # PPM to PNG conversion
            if len(sys.argv) == 4:
                try:
                    new_width = int(sys.argv[1])
                    new_height = int(sys.argv[2])
                    convert_ppm_to_png(file_path, new_size=(new_width, new_height))
                except ValueError:
                    print("Error: Width and height must be integers.")
            else:
                convert_ppm_to_png(file_path)
        else:
            print("Error: Input file must have .png or .ppm extension")
            print("Usage: python ppm.py [width] [height] <input_file.png|ppm>")
