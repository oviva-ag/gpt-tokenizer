package com.oviva.gpt.tokenizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GptTokenizerTest {
	private final String encodingExample = "Hello my name is Manuel.";
	private final List<Integer> decodingExample = Arrays.asList(15496, 616, 1438, 318, 25995, 13);

	private final String encodingLongTextExample = "interesting";
	private final List<Integer> decodingLongTextExample = Collections.singletonList(47914);

	private GptTokenizer tokenizer;

	@BeforeEach
	public void setUp() throws Exception {
		tokenizer = new GptTokenizer();
	}

	@Test
	public void test_encode() {
		List<Integer> result = tokenizer.encode(encodingExample);
		Assertions.assertEquals(decodingExample, result);
	}

	@Test
	public void test_decode()  {
		String result = tokenizer.decode(decodingExample);
		Assertions.assertEquals(encodingExample, result);
	}

	@Test
	public void test_longWord() {
		List<Integer> result = tokenizer.encode(encodingLongTextExample);
		Assertions.assertEquals(decodingLongTextExample, result);
	}

	@ParameterizedTest
	@CsvFileSource(resources = "/testcases.csv", numLinesToSkip = 1)
	void test_encode(String input, String expected) throws JsonProcessingException {

		List<Integer> expectedList = new ObjectMapper().readValue(expected, new TypeReference<List<Integer>>() {
		});

		assertEquals(expectedList, tokenizer.encode(input));
	}

}
