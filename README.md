# ProflPartialMatrix

FSE'21 Relevant Packages
* com.mycompany.patchstatistics
  * Contains main classes driving generation of FSE data
* com.mycompany.patchstatistics.tools
  * Contains tool-specific parsers to read patch info + patch test data
  
  ### Class GenerateComparisonStatistics
  Entry main point. Creates tool-specific parser and processes the data for each tool
  
  ### Class Patch
  Used to faciliate the comparison of patch characteristics between patches. The two comparisions currently use are if feature A = B or if collection B exists contains item A.
	* matching comparisons on GOOD_PATCHES = true positive
	* matching comparisons on BAD_PATCHES = true negatives
	* differing comparisons on GOOD_PATCHES = false negative
	* differeing comparisons on BAD_PATCHES = false positive
	
	Once all the patch characteristics / features have been compared, the various statistics (accuracy, recall, f1, etc) are computed
  
  ### Class Stats
	* Contains all the statistical metrics that patches may be sorted by
  
  ### Class PatchCharacteristic
	* Data class which contains all the characteristics / features per patch. Currently, every tool except prapr has the following features
		* Modified element(s) - Collection
		* Patch Category (not used in comparisions)
	* Prapr has the additional potential features; class, method, methodSig, lineNumber, susp, isInFinallyBlock, description, mutator (taken from prapr data files)
	

## Class Tool
The core method in this script is Tool.process(). After loading patch / test data, this method

	For each metric (i.e looking at the change in placement in PLAUSIBLE patches, NoisyFix or better patches, etc)
	1. Determines the baseline for the metric
	2. While any releveant patch has not been found (i.e PLAUSIBLE patches with the PLAUSIBLE metric)
		1. Pops the leading patch
		2. Compares leading patch to all other patches in the list
		3. Calculates new statistics per metric
		4. Reorder patches in descending order of statistic
	3. Determines the new baseline after sorting the set of patches
