//This script parses list of words and sets one word into a variable
//Script parameters:
//1 - String with comma-separated words
//2 - index of word in the list
//3 - name of variable into which the word will be set
def wordList = args[0].split(",") as List
def index = args[1] as Integer
def outputVariable = args[2]

def word = wordList.get(index % wordList.size())
vars.put(outputVariable, word);
SampleResult.setIgnore()
