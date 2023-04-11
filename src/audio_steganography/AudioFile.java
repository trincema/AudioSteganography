package audio_steganography;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * 
 */
public class AudioFile {
	private String filePath;
	private AudioInputStream inputStream;
	private AudioFormat audioFormat;
	
	/**
	 * @param filePath Audio file path.
	 */
	public AudioFile(String filePath) {
		this.filePath = filePath;
		try {
			this.inputStream = AudioSystem.getAudioInputStream(new File(filePath));
			this.audioFormat = this.inputStream.getFormat();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public AudioFormat getFormat() {
		return this.audioFormat;
	}
	
	public boolean isBigEndian() {
		return audioFormat.isBigEndian();
	}
	
	/**
	 * Checks if a given image data would fit to be encoded inside a given audio file.
	 */
	public boolean imageFitsAudio(byte[] imageBytes) {
		byte[] audioBytes = this.getSamples();
		if (audioBytes.length / (this.audioFormat.getSampleSizeInBits() / 8) < ((imageBytes.length + 4 + 3) * 8)) {
			return false;
		}
		return true;
	}
	
	public byte[] getSamples() {
		try {
			File file = new File(this.filePath);
	        if (file.exists()) {
	        	AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
	            int bytesToRead = inputStream.available();
	            byte[] bytes = new byte[bytesToRead];
	            int bytesRead = inputStream.read(bytes);
	            if (bytesToRead != bytesRead) {
	                throw new IllegalStateException("Read only " + bytesRead + " of " + bytesToRead + " bytes"); 
	            }
	            
	            return bytes;
	        }
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
            throw new IllegalArgumentException("Could not read '" + this.filePath + "'", ioe);
		}
		return new byte[0];
	}
	
	/**
	 * Saving the encoded audio file to disk.
	 */
	public void saveAudio(byte[] audioBytes, final File out) {
		try {
			PipedOutputStream pos = new PipedOutputStream();
			PipedInputStream pis = new PipedInputStream(pos);
			final AudioInputStream ais = new AudioInputStream(pis, this.audioFormat, AudioSystem.NOT_SPECIFIED);
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
			pos.write(audioBytes, 0, audioBytes.length);
			if (pos != null) {
	            ais.close();
	            pis.close();
	            pos.close();
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** Decode bytes of audioBytes into audioSamples.
	 */
    public double[] decodeBytes(byte[] audioBytes) {
    	double[] audioSamples = new double[audioBytes.length / this.audioFormat.getSampleSizeInBits() / 8];
        int sampleSizeInBytes = this.audioFormat.getSampleSizeInBits() / 8;
        int[] sampleBytes = new int[sampleSizeInBytes];
        int k = 0; // index in audioBytes
        for (int i = 0; i < audioSamples.length; i++) {
            // collect sample byte in big-endian order
            if (this.audioFormat.isBigEndian()) {
                // bytes start with MSB
                for (int j = 0; j < sampleSizeInBytes; j++) {
                    sampleBytes[j] = audioBytes[k++];
                }
            } else {
                // bytes start with LSB
                for (int j = sampleSizeInBytes - 1; j >= 0; j--) {
                    sampleBytes[j] = audioBytes[k++];
                    if (sampleBytes[j] != 0)
                        j = j + 0;
                }
            }
            // get integer value from bytes
            int ival = 0;
            for (int j = 0; j < sampleSizeInBytes; j++) {
                ival += sampleBytes[j];
                if (j < sampleSizeInBytes - 1) ival <<= 8;
            }
            // decode value
            double ratio = Math.pow(2., this.audioFormat.getSampleSizeInBits() - 1);
            double val = ((double) ival) / ratio;
            audioSamples[i] = val;
        }
        return audioSamples;
    }
	
	public void properties() {
		System.out.println("Channels: " + this.audioFormat.getChannels());
		System.out.println("Big endian: " + this.audioFormat.isBigEndian());
		System.out.println("Sample size: " + this.audioFormat.getSampleSizeInBits());
		System.out.println("Frame rate: " + this.audioFormat.getFrameRate());
		System.out.println("Frame size: " + this.audioFormat.getFrameSize());
		System.out.println("Encoded file size: " + new File(this.filePath).length() + " [bytes]");
	}
	
	public static void main(String[] args) {
		AudioFile audio = new AudioFile("./WAV/PCM 24 bit/pcm stereo 24 bit 44.1kHz.wav");
		audio.properties();
		
	}

}
