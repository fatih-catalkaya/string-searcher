# String searcher
A JavaFX demo using Java 17. This application loads a list of strings from a
text file (one string per line). The application continues to load the whole file
into a list, shuffles it and starts scanning the strings for a substring that is 
entered by the user.

Additionally, the user can decide to perform a multi-threaded scan by increasing
a thread counter. When multiple threads are used, the list is divided into equal
chunks that is then scanned by each thread.