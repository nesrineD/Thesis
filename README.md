# ConceptDetection
    The training and testing text need to be stored in the resources folder.
    The training texts need to be in the following format: 
      Sentence1. [imp1,imp2,..]. Sentence2. [imp1,imp2,..]. ... 
      and all sentences should be in one line. The annotated texts are all under the resources file. 
    - App.java: Main Class
    - Processor.java performs the whole program functionalities, it calls the methods from the Helper.java Class
    The paths to the Training and Testing texts and the graphml document are set in the Processor.java Class
    - GraphMLConverter.java:  generates the graphml document
    The part responsible for testing graph construction is commented out for the testing purposes.
    resources/test-text4.txt is a testing text 
    resources/testing.graphml is the graph obtained with text4.txt as training text and test-text4.txt as testing text
    In this graph the vertices corresponding to the testing text are in Red and the overlapping nodes are in Pink.
    resources/text4.graphml the graph corresonding to text4
    resources/NodesDegrees.txt:  a file containing the list of nodes and their degrees.


