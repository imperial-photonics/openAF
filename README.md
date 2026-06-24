# openAF
This repository is for the openAF AutofocusPlugin for Micro-Manager 2.0. It was originally published in the paper "openFrame: A modular, sustainable, open microscopy platform with single-shot, dual-axis optical autofocus module providing high precision and long range of operation" (https://onlinelibrary.wiley.com/doi/10.1111/jmi.13219). The repository as it was when the paper was published, with some additional license information, is available as the v1.0.0 release: https://github.com/ImperialCollegeLondon/openAF/releases/tag/v1.0.0

The current main branch of this repository contains additional capability updates made for the paper "Quantitative evaluation of LED based optical autofocus module", which is being prepared for publication, and are primarily intended to deal with power fluctiations in the light source.

## Software setup
You will need a functioning Python environment with the appropriate dependencies satisfied in order to run the Python code, but no extra software is needed (other than µManager) for the Java side.

## Setting up the software environment (Python)
We recommend the use of a virtual environment for Python in order to prevent dependancy issues. We have used Anaconda for this purpose, and these instructions assume that you have Anaconda installed, will make a virtual environment called openAF, and will use Spyder to run the python script.

To create a suitable Python environment using Anaconda, an environment file (openAF.yml) is available in the _openAF_Python_ folder of this repository. This can be used via the Anaconda prompt to create the environment. 

You should also ensure that you have installed the appropriate Spinnaker SDK with Python support (version 2.3.0.77) if you are using the Chameleon camera as in the original openAF implementation before trying to use the openAF code. You will also need to pip-install the Spinnaker wheel (version 2.3.0.77) into the openAF environment.

The Python part of the code is best run from Spyder (in the openAF environment), by running DefocusCalc.py

## Setting up software (Java)
We have used NetBeans to create the AutofocusPlugin for µManager. If you simply wish to use the plugin, just navigate to the _openAF_Java_ folder, then to the _dist_ subfolder, where you will find a file called [Open_AF.jar](https://github.com/ImperialCollegeLondon/openAF/blob/main/openAF_Java/dist/Open_AF.jar) - simply copy this to your Micro-Manager 2.0.0 "\mmautofocus" subdirectory, and it should be available the next time µManager is started. Warning: This may not work in some versions of Micro-manager if significant updates have been made. In this case, you will need to follow the instructions in "Setting up the software environment (Java)" in order to compile it against the version of µManager that you are using.

### Setting up the software environment (Java)
If you wish to modify the code for the Java side, you can do so by installing NetBeans and setting up a project for openAF. To do this, first install JDK8, then install NetBeans (version 18 or below). 

After this, you can simply download the _openAF_Java_ folder and load the project it contains into NetBeans. You may need to update the project properties to reflect the exact version of µManager that is installed on your computer, as some plugins and resources will have different version numbers. This is primarily done through picking the "Projects" tab in NetBeans, right-clicking on the Open_AF project name, and selecting "Properties". Click the "Libraries" item at the left of the popup window, select the "Compile" tab, and then remove all the files in the panel below. 
After that, use the "Add JAR/Folder" button, then add ij.jar from your µManager installation folder. Then use the "Add JAR/Folder" button again, navigating to the "plugins" folder. then the "Micro-Manager" folder inside that, and add *ALL* the .jar files in that folder. Finally, go to the "Run" item on the left hand side of the popup window, and change the "Working Directory" to your µManager installation folder. 
Following this, you should navigate to the "Files" tab at the top left of the NetBeans window. Expand the Open_AF item, and open the file called "build.xml". You will see a line near the top (probably line 14) that says something like:
<property name="pluginsDir"  location="C:\Program Files\Micro-Manager-2.0\mmautofocus" />
Change the location to be your µManager installation folder with \mmautofocus after it, between quotation marks.
You should now be able to press the 'build and clean' button in NetBeans, then a copy of the compiled OpenAF.jar file will be copied to the right place in your µManager installation, and you can run µManager and have the option to use openAF.

# Running openAF
In order to setup the openAF system in closed loop operation, the following steps should be taken (the instructions here assume the use of an oil immersion objective):

1.	Ensure the python script for openAF (DefocusCalc.py) is running and the AF light source is turned off
2.	Start µManager, and establish a socket connection to the Python process (Say "Yes" to the "Do you want to connect to Socket" popup) - you will see activity in the Spyder terminal window
3.	Place the sample to be imaged in the system and focus on the appropriate interface (typically the one between the coverslip and the sample mounting medium) using images from the standard imaging camera.
4.  Use the Z-stage to move the sample away from the objective by a distance significant enough for the reflection at the coverslip to be extremely spread out (e.g. 100µm for a 100x high NA oil objective)
5.	Set "OpenAF-Ref/Background Imgs" to “Set noise BG”, and a dark image is recorded
6.	Turn on the AF light source
7.	Set "OpenAF-Half Range" to a small value e.g. 1µm (this should be the default)
8.	Set "OpenAF-Step size" to double the value you have just put in the "OpenAF-Half Range" box (i.e. 2µm, which should also be the default)
9.	Set "OpenAF-Ref/Background Imgs" to “Set BG”, and a pair of background images will be recorded
10.	Bring the sample back into focus by moving the sample towards the objective by the same amount it was moved away in step 4
11.	Set "OpenAF-Noise Background" is set to “Set Infocus”, and an in-focus image will be recorded. The signal from the light source should not protrude into the edge 20% of the autofocus camera field of view. You can check this using the autofocus camera (Chameleon).
12. Set	"OpenAF-Half Range" to a value expected to cover the variations in focal plane across the sample (typically 15µm in the LED implementation, or potentially more with an SLD) and set "OpenAF-Stepsize" to a reasonably small value to ensure that focus can be maintained within the objective depth of field (Typically 0.1µm for a high NA 100x oil objective). You may be able to set the stepsize value larger if using interpolation.
13. Set	OpenAF-Calibration to “On”. This will result in a z-stack of images being acquired on the autofocus camera (Activity is not shown other than outputs in the Python terminal). Once this is done, a graph showing the values obtained for the AF metric will be shown, as will a graph of the ratios between them. If smooth curves with peaks slightly offset from each other are shown for the blue and red curves, then this has been done successfully.
14.	Set the "OpenAF-FWHM Threshold" value (shown on the primary vertical axis - left hand side) to a metric value (y-axis) above where the sharper of the red and blue curves flattens off. By default, this is autoset to the lowest value of all the metric curves as a rough initial approximation.
15.	Set the "OpenAF-Intensity Threshold" value should set by finding the point on the x-axis (Defocus) where the low point of the green line on the graph (Mean intensity) is reached, and use the value on the secondary y-axis (Mean intensity - right hand side) and using that value
16.	Bring the sample to the desired focus position and set "OpenAF-DefineFocus" to “On”. This will set the focus plane, then the value will revert to "Off". This should only be done in the z-range covered by your metrics
17.	If you wish to use interpolation, set "OpenAF-Interpolation on" to "On"
18.	Finally, closed-loop operation can be enabled/disabled by setting OpenAF-Continuous Focus to “On” or “Off respectively”
19.	If a different focus position is needed, "OpenAF-Continous Focus" should be set to “Off”, and steps 17 and 18 repeated
20.	At any time, you can set the "OpenAF-Disable Z" to disable correction, in order to allow characterisation of drift without the autofocus in operation
21.	If you wish to record a list of Z positions and timestamps, along with information on whether the autofocus Z control was disabled, set the "OpenAF-Finalise and save Z-list" to "Now". This will save everything from the first time "OpenAF-Continous Focus" is set to "On" until now. It cannot be used more than once without restarting the python code (restart the kernel) and µManager. In addition, the AF metrics are saved as a seperate file on the python side continously as they are being computed 
