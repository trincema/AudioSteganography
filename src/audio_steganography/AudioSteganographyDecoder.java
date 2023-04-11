package audio_steganography;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;

public class AudioSteganographyDecoder {
	private AudioFormat audioFormat;
	
	private static final int IMG_LENGTH = 4 * 8;
	
	/**
	 * Decoding a given encoded input image.
	 * @param encodedPath The path of the encoded image to decode.
	 */
	public void decode(String audioFilePath) {
        AudioFile audioFile = new AudioFile(audioFilePath);
        this.audioFormat = audioFile.getFormat();
        DecodeData decodeData = decodeImage(audioFile.getSamples());
        saveDecodedImage(decodeData.getImgBytes(), audioFilePath, decodeData.getImgExtension());
    }
	
	/**
	 * Decoding the encoded image in the byte data of the original image.
	 * @param originalBytes The byte data of the original image.
	 * @return The byte data of the decoded image.
	 */
	private DecodeData decodeImage(byte[] audioSamples) {
        int k = 0;
        
        // Loop through 32 bytes of data to determine image length
        long length = 0;
        for(int i = 0; i < IMG_LENGTH; ++i) {
            length = (length << 1) | (audioSamples[k] & 1);
            k += this.audioFormat.getSampleSizeInBits() / 8;
        }
        
        // Loop through 24 bytes of data to determine image extension
        byte[] imgExtBytes = new byte[3];
        for (int b = 0; b < imgExtBytes.length; b++) {
        	for(int j = 0; j < 8; ++j) {
        		imgExtBytes[b] = (byte)((imgExtBytes[b] << 1) | (audioSamples[k] & 1));
                k += this.audioFormat.getSampleSizeInBits() / 8;
            }
        }
        String imgExtension = new String(imgExtBytes);
        
        // Loop through each byte of image
        byte[] imgBytes = new byte[(int)length];
        for(int b = 0; b < imgBytes.length; ++b ) {
            // Loop through each bit within a byte of text
            for(int i = 0; i < 8; ++i) {
                // assign bit: [(new byte value) << 1] OR [(text byte) AND 1]
            	imgBytes[b] = (byte)((imgBytes[b] << 1) | (audioSamples[k] & 1));
                k += this.audioFormat.getSampleSizeInBits() / 8;
            }
        }
        
        DecodeData decodedData = new DecodeData(imgBytes, imgExtension);
        return decodedData;
    }
	
	/**
	 * Saving the decoded image byte data to disk.
	 * @param data Decoded image data.
	 */
	private void saveDecodedImage(byte[] data, String audioFilePath, String imgExtension) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			BufferedImage image = ImageIO.read(bis);
			String currentDir = new File(audioFilePath).getParent();
			String fileName = getFileName(audioFilePath);
			if (fileName.contains("encoded")) {
				fileName = fileName.replace("encoded", "decoded");
			} else {
				fileName += "_decoded";
			}
			String decodedPath = currentDir + File.separator + fileName + "." + imgExtension;
			if (ImageIO.write(image, imgExtension, new File(decodedPath))) {
				System.out.println("Image '" + new File(decodedPath).getName() + "' successfully decoded");
			}
			System.out.println("Decoded image size: " + new File(decodedPath).length() + " [bytes]");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
     * @return The file name (without extension) of a given file.
     */
    private String getFileName(String path) {
    	String fileName = new File(path).getName();
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
		    extension = fileName.substring(0, i);
		}
		return extension;
    }
    
	public static void main(String[] args) {
		AudioSteganographyDecoder decoder = new AudioSteganographyDecoder();
		decoder.decode("./encoded.wav");
	}

}
