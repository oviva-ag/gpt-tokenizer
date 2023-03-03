package com.oviva.gpt.tokenizer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GptTokenizer {
	public static final String ENCODER_FILE_NAME = "encoder.json";
	public static final String VOCAB_FILE_NAME = "vocab.bpe";

	private final Map<String, Object> encoder;
	private final Map<Object, String> decoder;

	private final Map<String, String> cache = new HashMap<>();
	private final Map<Integer, String> byte2unicode = byteToUnicode();
	private final Map<SimpleEntry<String, String>, Integer> bpeRanks = new HashMap<>();
	private final Pattern pattern = Pattern.compile(
			"'s|'t|'re|'ve|'m|'ll|'d| ?\\p{L}+| ?\\p{N}+| ?[^\\s\\p{L}\\p{N}]+|\\s+(?!\\S)|\\s+");

	public GptTokenizer() throws Exception {

		try {

			this.encoder = new ObjectMapper().readValue(
					getFileFromResources(ENCODER_FILE_NAME),
					new TypeReference<Map<String, Object>>() {
					}
			);
			this.decoder = encoder.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

			List<String> bpe = Files.readAllLines(Paths.get(getFileFromResources(VOCAB_FILE_NAME).toURI()));

			for (int i = 0; i < bpe.size(); i++) {
				String[] pairs = bpe.get(i).split(" ");
				this.bpeRanks.put(new SimpleEntry<>(pairs[0], pairs[1]), i);
			}
		} catch (IOException | URISyntaxException e) {
			throw new Exception("Unable to initialize tokenizer", e);
		}
	}

	public List<Integer> encode(String text) {
		Matcher matcher = pattern.matcher(text);
		List<String> unicodes = new ArrayList<>();
		List<Integer> bpeTokens = new ArrayList<>();

		while (matcher.find()) {
			String match = matcher.group();
			StringBuilder unicodeBuilder = new StringBuilder();
			for (byte b : match.getBytes(StandardCharsets.UTF_8)) {
				unicodeBuilder.append(this.byte2unicode.get((int) b));
			}
			unicodes.add(unicodeBuilder.toString());
		}

		for (String token : unicodes) {
			for (String bpeToken : bpe(token).split(" ")) {
				bpeTokens.add((Integer) encoder.get(bpeToken));
			}
		}

		return bpeTokens;
	}

	public String decode(List<Integer> tokens) {
		StringBuilder textBuilder = new StringBuilder();
		List<String> byteBufferList = new ArrayList<>();

		for (int token : tokens) {
			textBuilder.append(decoder.get(token));
		}
		String text = textBuilder.toString();

		for (int i = 0; i < text.length(); i++) {
			byteBufferList.add(byte2unicode.get((int) text.charAt(i)));
		}

		byte[] byteBuffer = new byte[byteBufferList.size()];
		for (int i = 0; i < byteBuffer.length; i++) {
			String byteString = byteBufferList.get(i);
			if (byteString == null) {
				byteString = " ";
			}
			byteBuffer[i] = (byte) byteString.charAt(0);
		}

		CharBuffer charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(byteBuffer));
		return charBuffer.toString();

	}

	private Set<SimpleEntry<String, String>> getPairs(List<String> word) {
		Set<SimpleEntry<String, String>> pairs = new HashSet<>();
		String prevCharacter = word.get(0);
		for (String character : word.subList(1, word.size())) {
			pairs.add(new SimpleEntry<>(prevCharacter, character));
			prevCharacter = character;
		}
		return pairs;
	}

	private Map<Integer, String> byteToUnicode() {
		List<Integer> bs = Stream.of(IntStream.range('!', '~' + 1).boxed(),
				IntStream.range('¡', '¬' + 1).boxed(),
				IntStream.range('®', 'ÿ' + 1).boxed()
		).reduce(Stream::concat).get().collect(Collectors.toList());
		List<Integer> cs = new ArrayList<>(bs);

		int n = 0;
		int max = (int) Math.pow(2, 8);
		for (int b = 0; b < max; b++) {
			if (!bs.contains(b)) {
				bs.add(b);
				cs.add(max + n);
				n += 1;
			}
		}
		List<String> csString = cs.stream().map(i -> String.valueOf(Character.toChars(i))).collect(Collectors.toList());

		Map<Integer, String> output = new HashMap<>();
		for (int i = 0; i < bs.size(); i++) {
			output.put(bs.get(i), csString.get(i));
		}
		return output;
	}

	private String bpe(String token) {
		if (cache.containsKey(token)) {
			return cache.get(token);
		}

		List<String> word = token.chars().mapToObj(i -> String.valueOf((char) i)).collect(Collectors.toList());

		Set<SimpleEntry<String, String>> pairs = getPairs(word);

		while (true) {
			int minScore = Integer.MAX_VALUE;
			SimpleEntry<String, String> biGram = null;

			for (SimpleEntry<String, String> pair : pairs) {
				if (bpeRanks.containsKey(pair)) {
					int score = bpeRanks.get(pair);

					if (score < minScore) {
						minScore = score;
						biGram = pair;
					}
				}
			}

			if (biGram == null) {
				break;
			}

			String first = biGram.getKey();
			String second = biGram.getValue();
			List<String> newWord = new ArrayList<>();
			int i = 0;

			while (i < word.size()) {
				int j = indexWithStartPosition(word, first, i);

				if (j != -1) {
					newWord.addAll(word.subList(i, j));
					i = j;
				} else {
					newWord.addAll(word.subList(i, word.size()));
					break;
				}

				if (word.get(i).equals(first) && i < word.size() - 1 && word.get(i + 1).equals(second)) {
					newWord.add(first + second);
					i += 2;
				} else {
					newWord.add(word.get(i));
					i += 1;
				}
			}

			word = newWord;
			if (word.size() == 1) {
				break;
			} else {
				pairs = getPairs(word);
			}
		}

		String output = String.join(" ", word);
		cache.put(token, output);
		return output;
	}

	private <T> int indexWithStartPosition(List<T> list, T find, int startPosition) {
		if (list == null || list.isEmpty()) {
			return -1;
		}
		for (int index = startPosition; index < list.size(); index++) {
			if (list.get(index).equals(find)) {
				return index;
			}
		}
		return -1;
	}



	public static File getFileFromResources(String fileName) throws URISyntaxException {

		ClassLoader classLoader = GptTokenizer.class.getClassLoader();
		URL resource = classLoader.getResource(fileName);

		if (resource == null) {
			throw new IllegalArgumentException("file not found! " + fileName);
		} else {
			return new File(resource.toURI());
		}
	}
}