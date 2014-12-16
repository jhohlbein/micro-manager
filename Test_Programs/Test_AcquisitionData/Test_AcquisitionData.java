import org.micromanager.metadata.AcquisitionData;
import org.micromanager.metadata.MMAcqDataException;

/* Test_AcquisitionData.java
 * Created on Jun 27, 2006
 *
 * MicroManager sample code
 */

/**
 * Example program to demonstrate accessing acquired data generated by MicroManager.
 * The program uses AcquistionData class to access a collection of files stored
 * on disk, in such way that they appear as a single multi-dimensional image object.
 * 
 * Requires MMJ_.jar and ij.jar on the classpath.
 */
public class Test_AcquisitionData {

   public static void main(String[] args) {
      
      final String acqDir = "c:/AcquisitionData/test_save/direct_0";
      
      // instantiate 5d image object
      AcquisitionData ad = new AcquisitionData();
      
      try {
         
         // "load" is a critical operation where the metadata is parsed and
         // all internal data structures are initialized. 
         // Image data, however, is not loaded at this point so there is no memory
         // limit to the size of the acqired data
         ad.load(acqDir);
         
         // get basic acquisition parameters
         int height = ad.getImageHeight();
         int width = ad.getImageWidth();
         int numFrames = ad.getNumberOfFrames();
         int numChannels = ad.getNumberOfChannels();
         int numSlices = ad.getNumberOfSlices();
         int depth = ad.getPixelDepth();
         
         System.out.println(acqDir + " contains:");
         System.out.println(numFrames + " frames, " + numChannels + " channels, " + numSlices + " slices, at " +
                            width + "X" + height + "X" + depth + " pixels");
         
         // Print full summary data associated with the acquisition
         System.out.println("\nSummary data:");
         String summaryData[] = ad.getSummaryKeys(); // list of all available properties
         for (int i=0; i<summaryData.length; i++) {
            String value = ad.getSummaryValue(summaryData[i]);
            System.out.println("   " + summaryData[i] + "=" + value);
         }
         
         // display channel names
         String channels[] = ad.getChannelNames();
         System.out.println("\nAvailable channels:");
         for (int i=0; i<channels.length; i++)
            System.out.println("   " + channels[i]);
         
         
         // accessing image data at the (frame, channel, slice) position specified below...
         int frame = 0;
         int channel = 0;
         int slice = 0;
         
         // Print full data associated with the specified image
         System.out.println("\nImage data at position (" + frame + "," + channel + "," + slice + ")");
         String imageData[] = ad.getImageKeys(frame, channel, slice);
         for (int i=0; i<imageData.length; i++) {
            String value = ad.getImageValue(frame, channel, slice, imageData[i]);
            System.out.println("   " + imageData[i] + "=" + value);
         }
         
         // obtain the image pixels at (frame,channel,slice) position
         // this is where the actual file is opened and the pixels
         // transfered to the memory
         Object pixelBlob = ad.getPixels(frame, channel, slice);
         
         // depending on the pixel type the appropriate pixel arrays are constructed
         int bytesRetrieved = 0;
         if (depth == 1) {
            // 8bit pixels
            byte[] img = (byte[]) pixelBlob;
            bytesRetrieved = img.length;
         } else if (depth == 2) {
            // 16bit pixels
            short[] img = (short[]) pixelBlob;
            bytesRetrieved = img.length * 2;
         }
         System.out.println("\nRetrieved " + bytesRetrieved + " bytes at position (" +
                            frame + "," + channel + "," + slice + ")");
         
      } catch (MMAcqDataException e) {
         e.printStackTrace();
      }
   }
}