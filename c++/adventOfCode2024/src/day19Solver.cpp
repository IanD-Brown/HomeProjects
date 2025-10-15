#include "day19Solver.h"

#include <climits>
#include <iostream>
#include <map>
#include <set>

using namespace std;

day19Solver::day19Solver(const string &testFile):
	solver(testFile) {}

void day19Solver::addPattern(const std::string &pattern) {
	auto fnd(m_patterns.find(pattern[0]));
	if (fnd == m_patterns.end()) {
		m_patterns[pattern[0]] = {pattern};
	} else {
		fnd->second.push_back(pattern);
	}
}

void day19Solver::loadData(const string &line) {
	if(line.empty()) {
		return;
	}
	if(line.find(", ") != string::npos) {
		size_t start(0);
		size_t end(0);
		size_t len(2);

		while((end = line.find(", ", start)) != string::npos) {
			addPattern(line.substr(start,end - start));
			start = end + len;
		}
		addPattern(line.substr(start));
	} else {
		m_data.push_back(line);
	}
}

solveResult day19Solver::matchString(const string &data, size_t pos,
									 map<string, solveResult> &endCache) const {
	auto cached(endCache.find(data.substr(pos)));
	if (cached != endCache.end()) {
		return cached->second;
	}
	auto fnd(m_patterns.find(data[pos]));
	solveResult matchCount(0);

	if (fnd != m_patterns.cend()) {
		for (const string& pattern : fnd->second) {
			if (pattern.size() <= data.size() - pos) {
				bool partMatch(true);
				for (size_t p(1); p < pattern.size(); ++p) {
					if (data[pos + p] != pattern[p]) {
						partMatch = false;
						break;
					}
				}

				if (partMatch) {
					if (pattern.size() + pos == data.size()) {
						++matchCount;
					} else {
						matchCount += matchString(data, pos + pattern.size(), endCache);
					}
					if (m_part1 && matchCount > 0) {
						return matchCount;
					}
				}
			}
		}
	}

	endCache[data.substr(pos)] = matchCount;
	return matchCount;
}

void day19Solver::clearData() {
	m_data.clear();
	m_patterns.clear();
}

solveResult day19Solver::compute() {
	solveResult count(0);
	for(const string &d : m_data) {
		map<string, solveResult> cache;
		solveResult matchCount(matchString(d, 0, cache));
		if (m_part1 && matchCount > 0) {
			++count;
		} else if (matchCount > 0) {
			count += matchCount;
		}
	}
	return count;
}

void day19Solver::loadTestData() {
	m_test = true;

	clearData();

	loadData("r, wr, b, g, bwu, rb, gb, br");
	loadData("");
	loadData("brwrr");
	loadData("bggr");
	loadData("gbbr");
	loadData("rrbgbr");
	loadData("ubwu");
	loadData("bwurrg");
	loadData("brgr");
	loadData("bbrgwb");
}
