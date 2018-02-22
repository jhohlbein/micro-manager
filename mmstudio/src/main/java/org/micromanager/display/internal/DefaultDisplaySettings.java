///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     Display implementation
//-----------------------------------------------------------------------------
//
// AUTHOR:       Chris Weisiger, 2015
//
// COPYRIGHT:    University of California, San Francisco, 2015
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package org.micromanager.display.internal;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;
import org.micromanager.UserProfile;
import org.micromanager.data.internal.PropertyKey;
import org.micromanager.display.ChannelDisplaySettings;
import org.micromanager.display.ComponentDisplaySettings;
import org.micromanager.display.DisplaySettings;
import org.micromanager.internal.utils.UserProfileStaticInterface;
import org.micromanager.internal.utils.MDUtils;
import org.micromanager.internal.utils.ReportingUtils;

public final class DefaultDisplaySettings implements DisplaySettings {
   private final double zoom_;
   private final double fps_;
   private final ColorMode mode_;
   private final boolean uniformChannelScaling_;
   private final boolean autostretch_;
   private final boolean useROI_;
   private final double extremaQuantile_;
   private final List<ChannelDisplaySettings> channelSettings_;

   private static class Builder implements DisplaySettings.Builder {
      private double zoom_ = 1.0;
      private double fps_ = 10.0;
      private ColorMode mode_ = ColorMode.GRAYSCALE;
      private boolean useUniformChannelScaling_ = false;
      private boolean autostretch_ = true;
      private boolean useROI_ = true;
      private double extremaQuantile_ = 0.1;
      private List<ChannelDisplaySettings> channelSettings_ =
            new ArrayList<ChannelDisplaySettings>();

      private Builder() {
         channelSettings_.add(DefaultChannelDisplaySettings.builder().build());
      }

      @Override
      public Builder zoomRatio(double ratio) {
         Preconditions.checkArgument(ratio > 0.0);
         zoom_ = ratio;
         return this;
      }

      @Override
      public Builder playbackFPS(double fps) {
         Preconditions.checkArgument(fps >= 0.0);
         fps_ = fps;
         return this;
      }

      @Override
      public Builder colorMode(ColorMode mode) {
         mode_ = mode;
         return this;
      }

      @Override
      public Builder colorModeComposite() {
         return colorMode(ColorMode.COMPOSITE);
      }

      @Override
      public Builder colorModeGrayscale() {
         return colorMode(ColorMode.GRAYSCALE);
      }

      @Override
      public Builder colorModeSingleColor() {
         return colorMode(ColorMode.COLOR);
      }

      @Override
      public Builder colorModeHighlightSaturated() {
         return colorMode(ColorMode.HIGHLIGHT_LIMITS);
      }

      @Override
      public Builder uniformChannelScaling(boolean enable) {
         useUniformChannelScaling_ = enable;
         return this;
      }

      @Override
      public Builder autostretch(boolean enable) {
         autostretch_ = enable;
         return this;
      }

      @Override
      public Builder roiAutoscale(boolean enable) {
         useROI_ = enable;
         return this;
      }

      @Override
      public Builder autoscaleIgnoredQuantile(double quantile) {
         Preconditions.checkArgument(quantile >= 0.0);
         extremaQuantile_ = quantile;
         return this;
      }

      @Override
      public Builder autoscaleIgnoredPercentile(double percentile) {
         return autoscaleIgnoredQuantile(0.01 * percentile);
      }

      @Override
      public Builder channel(int channel) {
         Preconditions.checkArgument(channel >= 0);
         while (channelSettings_.size() <= channel) {
            channelSettings_.add(DefaultChannelDisplaySettings.builder().build());
         }
         return this;
      }

      @Override
      public Builder channel(int channel, ChannelDisplaySettings settings) {
         channel(channel);
         channelSettings_.set(channel, settings);
         return this;
      }

      @Override
      public int getNumberOfChannels() {
         return channelSettings_.size();
      }

      @Override
      public ChannelDisplaySettings getChannelSettings(int channel) {
         Preconditions.checkArgument(channel >= 0);
         channel(channel);
         return channelSettings_.get(channel);
      }

      @Override
      public DisplaySettings build() {
         return new DefaultDisplaySettings(this);
      }
   }

   public static DisplaySettings.Builder builder() {
      return new Builder();
   }

   private DefaultDisplaySettings(Builder builder) {
      zoom_ = builder.zoom_;
      fps_ = builder.fps_;
      mode_ = builder.mode_;
      uniformChannelScaling_ = builder.useUniformChannelScaling_;
      autostretch_ = builder.autostretch_;
      useROI_ = builder.useROI_;
      extremaQuantile_ = builder.extremaQuantile_;
      channelSettings_ =
            new ArrayList<ChannelDisplaySettings>(builder.channelSettings_);
   }

   @Override
   public double getZoomRatio() {
      return zoom_;
   }

   @Override
   public double getPlaybackFPS() {
      return fps_;
   }

   @Override
   public ColorMode getColorMode() {
      return mode_;
   }

   @Override
   public boolean isUniformChannelScalingEnabled() {
      return uniformChannelScaling_;
   }

   @Override
   public boolean isAutostretchEnabled() {
      return autostretch_;
   }

   @Override
   public boolean isROIAutoscaleEnabled() {
      return useROI_;
   }

   @Override
   public double getAutoscaleIgnoredQuantile() {
      return extremaQuantile_;
   }

   @Override
   public double getAutoscaleIgnoredPercentile() {
      return 100.0 * extremaQuantile_;
   }

   @Override
   public int getNumberOfChannels() {
      return channelSettings_.size();
   }

   @Override
   public ChannelDisplaySettings getChannelSettings(int channel) {
      if (channel >= channelSettings_.size()) {
         return DefaultChannelDisplaySettings.builder().build();
      }
      return channelSettings_.get(channel);
   }

   @Override
   public List<ChannelDisplaySettings> getAllChannelSettings() {
      return new ArrayList<ChannelDisplaySettings>(channelSettings_);
   }

   @Override
   public List<Color> getAllChannelColors() {
      List<Color> ret = new ArrayList<Color>(getNumberOfChannels());
      for (ChannelDisplaySettings channelSettings : channelSettings_) {
         ret.add(channelSettings.getColor());
      }
      return ret;
   }

   @Override
   public Color getChannelColor(int channel) {
      return getChannelSettings(channel).getColor();
   }

   @Override
   public List<Boolean> getAllChannelVisibilities() {
      List<Boolean> ret = new ArrayList<Boolean>(getNumberOfChannels());
      for (ChannelDisplaySettings channelSettings : channelSettings_) {
         ret.add(channelSettings.isVisible());
      }
      return ret;
   }

   @Override
   public boolean isChannelVisible(int channel) {
      return getChannelSettings(channel).isVisible();
   }


   /**
    * This string is deprecated; it was used in prior versions of MM2.0 before
    * we converted the value from int to double.
    */
   private static final String ANIMATION_FPS_INT = "animationFPS";
   private static final String ANIMATION_FPS_DOUBLE = "animationFPS_Double";
   private static final String CHANNEL_COLOR_MODE = "channelColorMode";
   private static final String HISTOGRAM_UPDATE_RATE = "histogramUpdateRate";
   private static final String ZOOM_RATIO = "magnification";
   private static final String SHOULD_SYNC_CHANNELS = "shouldSyncChannels";
   private static final String SHOULD_AUTOSTRETCH = "shouldAutostretch";
   private static final String SHOULD_SCALE_WITH_ROI = "shouldScaleWithROI";
   private static final String EXTREMA_PERCENTAGE = "extremaPercentage";
   private static final String BIT_DEPTH_INDICES = "bitDepthIndices";
   private static final String SHOULD_USE_LOG_SCALE = "shouldUseLogScale";
   private static final String SHOULD_CALCULATE_STD_DEV = "shouldCalculateStdDev";
   private static final String USER_DATA = "userData";

   /**
    * Retrieve the display settings that have been saved in the preferences.
    * Note: we explicitly don't cache these settings, to ensure that
    * displays don't end up with copies of the same settings.
    * @param key String for storing settings under different locations, so
    *        different "types" of displays can have different default settings.
    */
   public static DefaultDisplaySettings getStandardSettings(String key) {
      UserProfile profile = UserProfileStaticInterface.getInstance();
      LegacyBuilder builder = new LegacyBuilder();
      // We have to convert colors to/from int arrays.
      // Note we assume RGB tuples in the colors array.
      // Seven colors because ImageJ only supports 7 channels; put yellow/cyan
      // first for colorblind-friendliness.
      Color[] defaultColors = new Color[] {Color.YELLOW, Color.CYAN,
         Color.MAGENTA, Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE};
      Integer[] defaultIntColors = colorsToInts(defaultColors);

      key = key + "_";
      // This value used to be an int, then got changed to a double, hence the
      // name change.
      builder.animationFPS(profile.getDouble(
               DefaultDisplaySettings.class,
               key + ANIMATION_FPS_DOUBLE, 10.0));
      builder.channelColorMode(
            DisplaySettings.ColorMode.fromInt(profile.getInt(
            DefaultDisplaySettings.class,
               key + CHANNEL_COLOR_MODE,
               DisplaySettings.ColorMode.COMPOSITE.getIndex())));
      builder.zoom(profile.getDouble(
            DefaultDisplaySettings.class,
               key + ZOOM_RATIO, 1.0));
      builder.shouldSyncChannels(profile.getBoolean(
            DefaultDisplaySettings.class,
               key + SHOULD_SYNC_CHANNELS, false));
      builder.shouldAutostretch(profile.getBoolean(
            DefaultDisplaySettings.class,
               key + SHOULD_AUTOSTRETCH, true));
      builder.shouldScaleWithROI(profile.getBoolean(
            DefaultDisplaySettings.class,
               key + SHOULD_SCALE_WITH_ROI, true));
      builder.extremaPercentage(profile.getDouble(
            DefaultDisplaySettings.class,
               key + EXTREMA_PERCENTAGE, 0.0));
      // Note we don't store user data in the prefs explicitly; let third-party
      // code manually access the prefs if they want.
      return builder.build();
   }

   /**
    * Set new settings in the user's profile.
    * @param key As with getStandardSettings, a specific key to use for
    *        this type of display.
    */
   public static void setStandardSettings(DisplaySettings settings,
         String key) {
      UserProfile profile = UserProfileStaticInterface.getInstance();
      key = key + "_";
      profile.setDouble(DefaultDisplaySettings.class,
            key + ANIMATION_FPS_DOUBLE,
            settings.getPlaybackFPS());
      if (settings.getChannelColorMode() != null) {
         profile.setInt(DefaultDisplaySettings.class,
               key + CHANNEL_COLOR_MODE,
               settings.getChannelColorMode().getIndex());
      }
      profile.setDouble(DefaultDisplaySettings.class,
            key + ZOOM_RATIO, settings.getZoomRatio());
      profile.setBoolean(DefaultDisplaySettings.class,
            key + SHOULD_SYNC_CHANNELS, settings.getShouldSyncChannels());
      profile.setBoolean(DefaultDisplaySettings.class,
            key + SHOULD_AUTOSTRETCH, settings.getShouldAutostretch());
      profile.setBoolean(DefaultDisplaySettings.class,
            key + SHOULD_SCALE_WITH_ROI, settings.getShouldScaleWithROI());
      profile.setDouble(DefaultDisplaySettings.class,
            key + EXTREMA_PERCENTAGE, settings.getExtremaPercentage());
   }

   /**
    * Return the current color mode setting in the profile, or the provided
    * default value. This is specifically available to allow the Snap/Live
    * Manager to default to grayscale instead of the normal default (returned
    * from getStandardSettings) of composite mode.
    * @param key Profile key to use, as per [get|set]StandardSettings.
    */
   public static ColorMode getStandardColorMode(String key,
         DisplaySettings.ColorMode defaultVal) {
      UserProfile profile = UserProfileStaticInterface.getInstance();
      key = key + "_";
      Integer mode = profile.getInt(DefaultDisplaySettings.class,
            CHANNEL_COLOR_MODE, null);
      if (mode == null) {
         return defaultVal;
      }
      return DisplaySettings.ColorMode.fromInt(mode);
   }

   /**
    * Convert the provided array of Colors to an array of Integers, in RGB
    * order.
    */
   private static Integer[] colorsToInts(Color[] colors) {
      Integer[] result = new Integer[colors.length * 3];
      for (int i = 0; i < colors.length; ++i) {
         result[i * 3] = colors[i].getRed();
         result[i * 3 + 1] = colors[i].getGreen();
         result[i * 3 + 2] = colors[i].getBlue();
      }
      return result;
   }

   /**
    * Reverse the process performed by colorsToInts().
    */
   private static Color[] intsToColors(Integer[] ints) {
      if (ints == null) {
         return null;
      }
      Color[] result = new Color[ints.length / 3];
      for (int i = 0; i < result.length; ++i) {
         int red = ints[i * 3];
         int green = ints[i * 3 + 1];
         int blue = ints[i * 3 + 2];
         result[i] = new Color(red, green, blue);
      }
      return result;
   }

   public static class DefaultContrastSettings implements DisplaySettings.ContrastSettings {
      Integer[] contrastMins_;
      Integer[] contrastMaxes_;
      Double[] gammas_;
      Boolean isVisible_;

      /**
       * Convenience method for single-component settings.
       */
      public DefaultContrastSettings(Integer contrastMin, Integer contrastMax,
            Double gamma, Boolean isVisible) {
         contrastMins_ = new Integer[] {contrastMin};
         contrastMaxes_ = new Integer[] {contrastMax};
         gammas_ = new Double[] {gamma};
         isVisible_ = isVisible;
      }

      public DefaultContrastSettings(Integer[] contrastMins,
            Integer[] contrastMaxes, Double[] gammas, Boolean isVisible) {
         contrastMins_ = contrastMins;
         contrastMaxes_ = contrastMaxes;
         gammas_ = gammas;
         isVisible_ = isVisible;
      }

      @Override
      public Integer[] getContrastMins() {
         return contrastMins_;
      }

      @Override
      public Integer getSafeContrastMin(int component, Integer defaultVal) {
         if (component < 0 || contrastMins_ == null ||
               contrastMins_.length <= component) {
            return defaultVal;
         }
         return contrastMins_[component];
      }

      @Override
      public Integer[] getContrastMaxes() {
         return contrastMaxes_;
      }

      @Override
      public Integer getSafeContrastMax(int component, Integer defaultVal) {
         if (component < 0 || contrastMaxes_ == null ||
               contrastMaxes_.length <= component) {
            return defaultVal;
         }
         return contrastMaxes_[component];
      }

      @Override
      public Double[] getContrastGammas() {
         return gammas_;
      }

      @Override
      public Double getSafeContrastGamma(int component, Double defaultVal) {
         if (component < 0 || gammas_ == null ||
               gammas_.length <= component) {
            return defaultVal;
         }
         return gammas_[component];
      }

      @Override
      public int getNumComponents() {
         int result = 0;
         if (contrastMins_ != null) {
            result = Math.max(result, contrastMins_.length);
         }
         if (contrastMaxes_ != null) {
            result = Math.max(result, contrastMaxes_.length);
         }
         if (gammas_ != null) {
            result = Math.max(result, gammas_.length);
         }
         return result;
      }

      @Override
      public Boolean getIsVisible() {
         return isVisible();
      }

      @Override
      public Boolean isVisible() {
         return isVisible_;
      }

      @Override
      public boolean equals(Object obj) {
         if (!(obj instanceof ContrastSettings)) {
            return false;
         }
         ContrastSettings alt = (ContrastSettings) obj;
         if (getNumComponents() != alt.getNumComponents()) {
            return false;
         }
         Integer[] altMins = alt.getContrastMins();
         Integer[] altMaxes = alt.getContrastMaxes();
         Double[] altGammas = alt.getContrastGammas();
         if (((contrastMins_ == null) != (altMins == null)) ||
               ((contrastMaxes_ == null) != (altMaxes == null)) ||
               ((gammas_ == null) != (altGammas == null))) {
            // Someone's array is null where the other one isn't.
            return false;
         }
         if ((contrastMins_ != null &&
               !Arrays.deepEquals(contrastMins_, altMins)) ||
               (contrastMaxes_ != null &&
                !Arrays.deepEquals(contrastMaxes_, altMaxes)) ||
               (gammas_ != null && !Arrays.deepEquals(gammas_, altGammas))) {
            // Arrays contain unequal values.
            return false;
         }
         // All arrays have same contents or are both null.
         return (isVisible_ != alt.getIsVisible());
      }

      @Override
      public String toString() {
         String result = String.format("<ContrastSettings (%d components)", getNumComponents());
         for (int i = 0; i < getNumComponents(); ++i) {
            result += String.format("(%d, %d @ %.2f)", getSafeContrastMin(i, -1), getSafeContrastMax(i, -1), getSafeContrastGamma(i, -1.0));
         }
         if (isVisible_ != null) {
            result += isVisible_ ? " (visible)" : " (hidden)";
         }
         return result + ">";
      }
   }

   @Deprecated
   public static class LegacyBuilder extends Builder
         implements DisplaySettings.DisplaySettingsBuilder
   {
      @Override
      public DefaultDisplaySettings build() {
         return new DefaultDisplaySettings(this);
      }

      @Override
      public DisplaySettingsBuilder channelColors(Color[] channelColors) {
         for (int i = 0; i < channelColors.length; ++i) {
            if (channelColors[i] == null) {
               continue;
            }
            channel(i, getChannelSettings(i).copyBuilder().
                  color(channelColors[i]).build());
         }
         return this;
      }

      @Override
      public DisplaySettingsBuilder safeUpdateChannelColor(Color newColor,
            int channelIndex) {
         channel(channelIndex, getChannelSettings(channelIndex).copyBuilder().
               color(newColor).build());
         return this;
      }

      @Override
      public DisplaySettingsBuilder channelContrastSettings(ContrastSettings[] contrastSettings) {
         if (contrastSettings == null) {
            return this;
         }
         for (int i = 0; i < contrastSettings.length; ++i) {
            if (contrastSettings[i] == null) {
               continue;
            }
            safeUpdateContrastSettings(contrastSettings[i], i);
         }
         return this;
      }

      @Override
      public DisplaySettingsBuilder safeUpdateContrastSettings(
            ContrastSettings legacySettings, int channelIndex) {
         if (legacySettings == null) {
            return this;
         }
         ChannelDisplaySettings channelSettings = getChannelSettings(channelIndex);
         ChannelDisplaySettings.Builder channelBuilder =
               channelSettings.copyBuilder();
         for (int j = 0; j < legacySettings.getNumComponents(); ++j) {
            ComponentDisplaySettings.Builder componentBuilder =
                  channelSettings.getComponentSettings(j).copyBuilder();
            if (legacySettings.getContrastMins() != null &&
                  legacySettings.getContrastMins()[j] != null) {
               componentBuilder = componentBuilder.scalingMinimum(
                     legacySettings.getContrastMins()[j]);
            }
            if (legacySettings.getContrastMaxes() != null &&
                  legacySettings.getContrastMaxes()[j] != null) {
               componentBuilder = componentBuilder.scalingMaximum(
                     legacySettings.getContrastMaxes()[j]);
            }
            if (legacySettings.getContrastGammas() != null &&
                  legacySettings.getContrastGammas()[j] != null) {
               componentBuilder = componentBuilder.scalingGamma(
                     legacySettings.getContrastGammas()[j]);
            }
            channelBuilder.component(j, componentBuilder.build());
         }
         if (legacySettings.getIsVisible() != null) {
            channelBuilder.visible(legacySettings.getIsVisible());
         }
         channel(channelIndex, channelBuilder.build());
         return this;
      }

      @Override
      public DisplaySettingsBuilder zoom(Double ratio) {
         if (ratio != null) {
            zoomRatio(ratio);
         }
         return this;
      }

      @Override
      @Deprecated
      public DisplaySettingsBuilder magnification(Double ratio) {
         return zoom(ratio);
      }

      @Override
      public DisplaySettingsBuilder animationFPS(Double animationFPS) {
         if (animationFPS != null) {
            playbackFPS(animationFPS);
         }
         return this;
      }

      @Override
      public DisplaySettingsBuilder channelColorMode(ColorMode channelColorMode) {
         if (channelColorMode != null) {
            colorMode(channelColorMode);
         }
         return this;
      }

      @Override
      public DisplaySettingsBuilder shouldSyncChannels(Boolean shouldSyncChannels) {
         if (shouldSyncChannels != null) {
            uniformChannelScaling(shouldSyncChannels);
         }
         return this;
      }

      @Override
      public DisplaySettingsBuilder shouldAutostretch(Boolean shouldAutostretch) {
         if (shouldAutostretch != null) {
            autostretch(shouldAutostretch);
         }
         return this;
      }

      @Override
      public DisplaySettingsBuilder shouldScaleWithROI(Boolean shouldScaleWithROI) {
         if (shouldScaleWithROI != null) {
            roiAutoscale(shouldScaleWithROI);
         }
         return this;
      }

      @Override
      public DisplaySettingsBuilder extremaPercentage(Double extremaPercentage) {
         if (extremaPercentage != null) {
            autoscaleIgnoredPercentile(extremaPercentage);
         }
         return this;
      }
   }

   @Override
   @Deprecated
   public Color[] getChannelColors() {
      Color[] ret = new Color[getNumberOfChannels()];
      for (int i = 0; i < ret.length; ++i) {
         ret[i] = getChannelSettings(i).getColor();
      }
      return ret;
   }

   @Override
   @Deprecated
   public Color getSafeChannelColor(int index, Color defaultVal) {
      if (index < 0 || index >= getNumberOfChannels()) {
         return defaultVal;
      }
      return getChannelSettings(index).getColor();
   }

   @Override
   public ContrastSettings[] getChannelContrastSettings() {
      ContrastSettings[] ret = new ContrastSettings[getNumberOfChannels()];
      for (int i = 0; i < getNumberOfChannels(); ++i) {
         ret[i] = getSafeContrastSettings(i, null);
      }
      return ret;
   }

   @Override
   public ContrastSettings getSafeContrastSettings(int index,
         ContrastSettings defaultVal) {
      if (index < 0 || index >= getNumberOfChannels()) {
         return defaultVal;
      }
      ChannelDisplaySettings channelSettings = getChannelSettings(index);
      int nComponents = channelSettings.getNumberOfComponents();
      Integer[] mins = new Integer[nComponents];
      Integer[] maxes = new Integer[nComponents];
      Double[] gammas = new Double[nComponents];
      for (int j = 0; j < nComponents; ++j) {
         ComponentDisplaySettings componentSettings =
               channelSettings.getComponentSettings(j);
         long min = componentSettings.getScalingMinimum();
         mins[j] = min > Integer.MAX_VALUE ? null : (int) min;
         long max = componentSettings.getScalingMaximum();
         maxes[j] = max > Integer.MAX_VALUE ? null : (int) max;
         gammas[j] = componentSettings.getScalingGamma();
      }
      return new DefaultContrastSettings(mins, maxes, gammas,
            channelSettings.isVisible());
   }

   @Override
   public Integer getSafeContrastMin(int index, int component,
         Integer defaultVal) {
      if (index < 0 || index >= getNumberOfChannels()) {
         return defaultVal;
      }
      ChannelDisplaySettings channelSettings = getChannelSettings(index);
      if (component < 0 || component >= channelSettings.getNumberOfComponents()) {
         return defaultVal;
      }
      long min = channelSettings.getComponentSettings(component).getScalingMinimum();
      return min > Integer.MAX_VALUE ? defaultVal : (int) min;
   }

   @Override
   public Integer getSafeContrastMax(int index, int component,
         Integer defaultVal) {
      if (index < 0 || index >= getNumberOfChannels()) {
         return defaultVal;
      }
      ChannelDisplaySettings channelSettings = getChannelSettings(index);
      if (component < 0 || component >= channelSettings.getNumberOfComponents()) {
         return defaultVal;
      }
      long max = channelSettings.getComponentSettings(component).getScalingMaximum();
      return max > Integer.MAX_VALUE ? defaultVal : (int) max;
   }

   @Override
   public Double getSafeContrastGamma(int index, int component,
         Double defaultVal) {
      if (index < 0 || index >= getNumberOfChannels()) {
         return defaultVal;
      }
      ChannelDisplaySettings channelSettings = getChannelSettings(index);
      if (component < 0 || component >= channelSettings.getNumberOfComponents()) {
         return defaultVal;
      }
      return channelSettings.getComponentSettings(component).getScalingGamma();
   }

   @Override
   public Boolean getSafeIsVisible(int index, Boolean defaultVal) {
      if (index < 0 || index >= getNumberOfChannels()) {
         return defaultVal;
      }
      return getChannelSettings(index).isVisible();
   }

   @Override
   @Deprecated
   public Double getMagnification() {
      return getZoomRatio();
   }

   @Override
   public Double getAnimationFPS() {
      return getPlaybackFPS();
   }

   @Override
   public DisplaySettings.ColorMode getChannelColorMode() {
      return getColorMode();
   }

   @Override
   public Boolean getShouldSyncChannels() {
      return null;
   }

   @Override
   public Boolean getShouldAutostretch() {
      return isAutostretchEnabled();
   }

   @Override
   public Boolean getShouldScaleWithROI() {
      return isROIAutoscaleEnabled();
   }

   @Override
   public Double getExtremaPercentage() {
      return getAutoscaleIgnoredPercentile();
   }

   @Override
   public DisplaySettings.Builder copyBuilder() {
      DisplaySettings.Builder ret = builder().zoomRatio(zoom_).playbackFPS(fps_).colorMode(mode_).
            uniformChannelScaling(uniformChannelScaling_).
            autostretch(autostretch_).roiAutoscale(useROI_).autoscaleIgnoredQuantile(extremaQuantile_);
      for (int i = 0; i < getNumberOfChannels(); ++i) {
         ret.channel(i, channelSettings_.get(i));
      }
      return ret;
   }

   @Override
   public DisplaySettings.Builder copyBuilderWithChannelSettings(int channel,
         ChannelDisplaySettings settings)
   {
      return copyBuilder().channel(channel, settings);
   }

   @Override
   public DisplaySettings.Builder copyBuilderWithComponentSettings(
         int channel, int component, ComponentDisplaySettings settings)
   {
      return copyBuilder().channel(channel,
            getChannelSettings(channel).
                  copyBuilderWithComponentSettings(component, settings).
                  build());
   }

   @Override
   @Deprecated
   public DisplaySettingsBuilder copy() {
      DisplaySettings.Builder ret = new LegacyBuilder().zoomRatio(zoom_).playbackFPS(fps_).colorMode(mode_).
            uniformChannelScaling(uniformChannelScaling_).
            autostretch(autostretch_).roiAutoscale(useROI_).autoscaleIgnoredQuantile(extremaQuantile_);
      for (int i = 0; i < getNumberOfChannels(); ++i) {
         ret.channel(i, channelSettings_.get(i));
      }
      return (LegacyBuilder) ret;
   }

   // TODO This should go in NonPropertyMapJSONFormats.DisplaySettings
   public static DefaultDisplaySettings legacyFromJSON(JSONObject tags) {
      if (tags == null) {
         return new LegacyBuilder().build();
      }
      try {
         LegacyBuilder builder = new LegacyBuilder();
         // Check for both methods of storing colors (see toJSON, below)
         if (MDUtils.hasChannelColor(tags)) {
            builder.channelColors(new Color[] {new Color(MDUtils.getChannelColor(tags))});
         }
         if (tags.has("ChColors")) {
            JSONArray colorTags = tags.getJSONArray("ChColors");
            Color[] colors = new Color[colorTags.length()];
            for (int i = 0; i < colorTags.length(); ++i) {
               colors[i] = new Color(colorTags.getInt(i));
            }
            builder.channelColors(colors);
         }

         // Reconstruct the channel contrast settings into ContrastSettings
         // objects. Note that gamma and channel visibility are not preserved
         // currently, let alone multi-component values.
         Integer[] minsArr = null;
         Integer[] maxesArr = null;
         if (tags.has("ChContrastMin")) {
            JSONArray mins = tags.getJSONArray("ChContrastMin");
            minsArr = new Integer[mins.length()];
            maxesArr = new Integer[mins.length()];
            for (int i = 0; i < minsArr.length; ++i) {
               minsArr[i] = mins.getInt(i);
            }
         }
         if (tags.has("ChContrastMax")) {
            JSONArray maxes = tags.getJSONArray("ChContrastMax");
            maxesArr = new Integer[maxes.length()];
            if (minsArr == null) {
               minsArr = new Integer[maxes.length()];
            }
            for (int i = 0; i < maxesArr.length; ++i) {
               maxesArr[i] = maxes.getInt(i);
            }
         }
         if (minsArr != null) {
            ArrayList<ContrastSettings> contrastSettings = new ArrayList<ContrastSettings>();
            for (int i = 0; i < minsArr.length; ++i) {
               Integer min = minsArr[i];
               Integer max = maxesArr[i];
               contrastSettings.add(
                     new DefaultContrastSettings(min, max, 1.0, true));
            }
            builder.channelContrastSettings(
                  contrastSettings.toArray(new DefaultContrastSettings[] {}));
         }

         if (tags.has(CHANNEL_COLOR_MODE)) {
            builder.channelColorMode(ColorMode.fromInt(
                     tags.getInt(CHANNEL_COLOR_MODE)));
         }
         if (tags.has(ZOOM_RATIO)) {
            builder.zoomRatio(tags.getDouble(ZOOM_RATIO));
         }
         if (tags.has(ANIMATION_FPS_DOUBLE)) {
            builder.playbackFPS(tags.getDouble(ANIMATION_FPS_DOUBLE));
         }
         if (tags.has(SHOULD_SYNC_CHANNELS)) {
            builder.shouldSyncChannels(tags.getBoolean(SHOULD_SYNC_CHANNELS));
         }
         if (tags.has(SHOULD_AUTOSTRETCH)) {
            builder.shouldAutostretch(tags.getBoolean(SHOULD_AUTOSTRETCH));
         }
         if (tags.has(SHOULD_SCALE_WITH_ROI)) {
            builder.shouldScaleWithROI(tags.getBoolean(SHOULD_SCALE_WITH_ROI));
         }
         if (tags.has(EXTREMA_PERCENTAGE)) {
            builder.extremaPercentage(tags.getDouble(EXTREMA_PERCENTAGE));
         }
         return builder.build();
      }
      catch (JSONException e) {
         ReportingUtils.logError(e, "Couldn't convert JSON into DisplaySettings");
         return null;
      }
   }

   @Override
   public String toString() {
      Field[] fields = getClass().getDeclaredFields();
      String result = "<DisplaySettings " + hashCode() + ": ";
      for (Field field : fields) {
         if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            continue;
         }
         try {
            Object val = field.get(this);
            if (val == null) {
               val = "null";
            }
            result += String.format("\n    %s: %s", field.getName(), val.toString());
         }
         catch (IllegalAccessException e) {
            ReportingUtils.logError(e, "Couldn't access field " + field.getName());
         }
      }
      result += ">";
      return result;
   }

   public PropertyMap toPropertyMap() {
      List<PropertyMap> channelSettings = new ArrayList<PropertyMap>();
      for (ChannelDisplaySettings cs : channelSettings_) {
         channelSettings.add(((DefaultChannelDisplaySettings) cs).toPropertyMap());
      }
      
      return PropertyMaps.builder().
            putDouble(PropertyKey.ZOOM_RATIO.key(), zoom_).
            putDouble(PropertyKey.PLAYBACK_FPS.key(), fps_).
            putEnumAsString(PropertyKey.COLOR_MODE.key(), mode_).
            putBoolean(PropertyKey.UNIFORM_CHANNEL_SCALING.key(), uniformChannelScaling_).
            putBoolean(PropertyKey.AUTOSTRETCH.key(), autostretch_).
            putBoolean(PropertyKey.ROI_AUTOSCALE.key(), useROI_).
            putDouble(PropertyKey.ACUTOSCALE_IGNORED_QUANTILE.key(), extremaQuantile_).
            putPropertyMapList(PropertyKey.CHANNEL_SETTINGS.key(), channelSettings).
            build();
   }
}