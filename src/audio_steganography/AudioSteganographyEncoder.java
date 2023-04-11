package audio_steganography;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.swing.JOptionPane;


public class AudioSteganographyEncoder {
	private AudioFormat audioFormat;
	
	/**
	 * Encoding an image inside an audio file.
	 * @param 
	 */
	public void encode(String audioFilePath, String imageFilePath, String outputPath, boolean logEnabled) {
		System.out.println("Audio file size: " + new File(audioFilePath).length() + " [bytes]");
		System.out.println("Image file size: " + new File(imageFilePath).length() + " [bytes]");
		
        AudioFile audioFile = new AudioFile(audioFilePath);
        this.audioFormat = audioFile.getFormat();
        BufferedImage image = readImage(imageFilePath);
        String imageExtension = getExtension(imageFilePath);
        byte[] imageBytes = getImageBytes(image, imageExtension);
        if (audioFile.imageFitsAudio(imageBytes)) {
            byte[] encodedSamples = embedImage(audioFile.getSamples(), imageBytes, imageExtension);
            audioFile.saveAudio(encodedSamples, new File(outputPath));
            if (logEnabled) {
            	log(audioFile.decodeBytes(audioFile.getSamples()), audioFile.decodeBytes(encodedSamples));
            }
        } else {
        	JOptionPane.showMessageDialog(null,
            		"Target audio file too small to hold the image!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
	
	/**
	 * Embedding the image into the audio file data.
	 * @param audioSamples The audio file samples as a double array with values between -1.0 and +1.0.
	 * @param image Data of the image to hide.
	 * @param imageExtension The extension of the image to hide.
	 * @return 
	 */
	private byte[] embedImage(byte[] audioSamples, byte[] imageBytes, String imageExtension) {
        byte[] imgLength = intToBytes(imageBytes.length);
        byte[] imgExtension = imageExtension.getBytes();
        try {
        	encodeImage(audioSamples, imgLength, 0); 		// 0 first positiong
        	encodeImage(audioSamples, imgExtension, imgLength.length * 8 * (this.audioFormat.getSampleSizeInBits() / 8));
        	encodeImage(audioSamples, imageBytes, ((imgLength.length + imgExtension.length) * 8) * (this.audioFormat.getSampleSizeInBits() / 8));
        } catch(Exception e) {
        	e.printStackTrace();
            JOptionPane.showMessageDialog(null,
            		"Target File cannot hold message!", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return audioSamples;
    }
	
	/**
	 * 
	 * Little-endian 8-bits: go from byte to byte in increments of 1.
	 * Little-endian 16-bits: go from LSB byte to next LSB byte in increments of 2.
	 * Little-endian 24-bits: go from LSB byte to next LSB byte in increments of 3.
	 * Big-endian 8 bits: go from byte to byte in increments of 1.
	 * Big-endian 16-bits: 
	 */
	private void encodeImage(byte[] audioBytes, byte[] additionBytes, int offset) {
		if (this.audioFormat.isBigEndian()) {
			// If big endian start directly from the MSB (depending on the sample bits length)
			offset += this.audioFormat.getSampleSizeInBits() / 8;
		}
		// Loop through each addition byte (image byte)
        for(int i = 0; i < additionBytes.length; ++i) {
            // Loop through the 8 bits of each byte
            int add = additionBytes[i];
            // Ensure the new offset value carries on through both loops
            for(int bit = 7; bit >= 0; --bit) {
                // assign an integer to b, shifted by bit spaces AND 1
                // a single bit of the current byte
                int b = (add >>> bit) & 1;
                // assign the bit by taking: [(previous byte value) AND 0xfe] OR bit to add
                // changes the last bit of the byte in the image to be the bit of addition
                audioBytes[offset] = (byte)((audioBytes[offset] & 0xFE) | b );
                offset += this.audioFormat.getSampleSizeInBits() / 8;
            }
        }
    }
	
	/**
	 * Reading an image from disk.
	 * @param imagePath The path of the image to read.
	 * @return A BufferedImage object with the image data.
	 */
	private BufferedImage readImage(String imagePath) {
        BufferedImage image = null;
        File file = new File(imagePath);
        try {
            image = ImageIO.read(file);
        } catch(Exception ex) {
        	ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Image could not be read from disk!", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return image;
    }
	
	/**
	 * Getting the bytes of an image read from disk.
	 * @param image The target image object.
	 * @param imageExtension The extension of the given image.
	 */
	private byte[] getImageBytes(BufferedImage image, String imageExtension) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write( image, imageExtension, baos );
			baos.flush();
	        byte[] imageInByte = baos.toByteArray();
	        baos.close();
	        return imageInByte;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new byte[0];
	}
	
	private void log(double[] originalSamples, double[] encodedSamples) {
		if (originalSamples.length == encodedSamples.length) {
			try {
				FileWriter log = new FileWriter("./log.csv");
				log.write("Original,Encoded\r\n");
				for (int i = 0; i < originalSamples.length; i++) {
					log.write(originalSamples[i] + "," + encodedSamples[i] + "\r\n");
				}
				log.flush();
				log.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
     * Generates proper byte format of an integer.
     */
    private byte[] intToBytes(int i) {
        byte byte3 = (byte)((i & 0xFF000000) >>> 24);
        byte byte2 = (byte)((i & 0x00FF0000) >>> 16);
        byte byte1 = (byte)((i & 0x0000FF00) >>> 8 );
        byte byte0 = (byte)((i & 0x000000FF)       );
        return(new byte[] { byte3, byte2, byte1, byte0 });
    }
    
    /**
     * @return The extension of the given file.
     */
	private String getExtension(String path) {
		String fileName = new File(path).getName();
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
		    extension = fileName.substring(i + 1);
		}
		return extension;
	}
	
	public static void main(String[] args) {
		AudioSteganographyEncoder steganography = new AudioSteganographyEncoder();
		String audioPath = "./WAV/PCM 24 bit/pcm mono 24 bit 44.1kHz.wav";
    	String imagePath = "./emoji.png";
		steganography.encode(audioPath, imagePath, "encoded.wav", true);
	}
}
