#include <iostream>

#include "day11Solver.h"

using namespace std;

static int iterationCount = 25;

day11Solver::day11Solver(const string& testFile) : solver(testFile) {
}

solveResult day11Solver::calcCount(solveResult seed, int iteration) {
	if ( iteration == iterationCount ) {
		return 1;
	}
	auto p = make_pair(seed, iteration);
	auto fnd = m_resultCache.find(p);
	if ( fnd != m_resultCache.end() ) {
		return fnd->second;
	}
	solveResult r;
	if ( seed == 0 ) {
		r = calcCount(1, iteration + 1);
	} else {
		string s = to_string(seed);

		if ( s.length() % 2 == 0 ) {
			r = calcCount(stoll(s.substr(0, s.length() / 2)), iteration + 1) + calcCount(stoll(s.substr(s.length() / 2)), iteration + 1);
		} else {
			r = calcCount(seed * 2024, iteration + 1);
		}
	}
	m_resultCache[ p ] = r;
	return r;
}

void day11Solver::loadData(const string& line) {
	size_t pos_start = 0, pos_end;

	while ( ( pos_end = line.find(" ", pos_start) ) != string::npos ) {
		m_data.push_back(stoll(line.substr(pos_start, pos_end - pos_start)));
		pos_start = pos_end + 1;
	}
	m_data.push_back(stoll(line.substr(pos_start)));
}

void day11Solver::clearData() {
	m_data.clear();
	m_resultCache.clear();
}

solveResult day11Solver::compute() {
	solveResult t = 0;

	for ( auto f : m_data ) {
		t += calcCount(f, 0);
	}

	return t;
}

solveResult day11Solver::compute2() {
	iterationCount = 75;
    return compute();
}

void day11Solver::loadTestData() {
	clearData();

	loadData("125 17");
}
