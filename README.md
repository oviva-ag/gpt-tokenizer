# gpt-tokenizer
Java implementation of GPT family tokenizer

This code is based on the work of Kevin Ko published at https://github.com/hyunwoongko/gpt2-tokenizer-java.

The main differences are:
- removal of some dependencies & using built in data structures
- use of Maven

## How to build the Maven library

```mvn install```

## Usage

Add this library to your maven dependencies:
```
<dependency>
  <groupId>com.oviva.gpt.tokenizer</groupId>
  <artifactId>gpt-tokenizer</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```
Then use it for example like this:

```List<Integer> tokenIds = new GptTokenizer().encode("Hello world")```

or

```String result = new GptTokenizer().decode(Arrays.asList(15496, 616, 1438, 318, 25995, 13))```

You can compare the results with the https://platform.openai.com/tokenizer.
