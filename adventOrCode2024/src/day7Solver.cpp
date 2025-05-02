#include <cassert>
#include <iostream>
#include <algorithm>
#include <map>
#include <sstream>

#include "day7Solver.h"

enum lineState {GOOD, BAD, UNKNOWN};

using namespace std;

static bool s_part2 = false;

day7Solver::day7Solver(const string& testFile) : solver(testFile) {
}

void day7Solver::loadData(const string& line) {
  m_data.push_back(line);
}

void day7Solver::clearData() {
  m_data.clear();
}

static lineState calc(solveResult target, solveResult prior, int index, const vector<int> values) {
	int limit = s_part2 ? 3 : 2;
	int badCount = 0;
	stringstream ss;

	for ( int op = 0; op < limit; ++op ) {
		unsigned long long next;
		switch ( op ) {
		case 0:
			next = prior * values[ index ];
			break;
		case 1:
			next = prior + values[ index ];
			break;
		case 2:
			ss << prior << values[ index ];
			ss >> next;
			break;
		}

		if ( next > target || next < target && index == values.size() - 1) {
			++badCount;
			continue;
		}
		if ( next == target && index == values.size() - 1 ) {
			return GOOD;
		}

		switch ( calc(target, next, index + 1, values) ) {
		case GOOD:
			return GOOD;
		case BAD:
			++badCount;
			break;
		}
	}

	return badCount < limit ? UNKNOWN : BAD;
}

void day7Solver::computeLine(const string& line, solveResult* dest) {
	size_t pos = line.find(':');
	solveResult t = stoll(line.substr(0, pos));
	vector<int> values = asVectorInt(line.substr(pos + 2), " ");
	if ( calc(t, values[0], 1, values) == GOOD ) {
		( *dest ) += t;
	}
}

solveResult day7Solver::compute() {
	solveResult t = 0;

	for ( const auto& line : m_data ) {
		computeLine(line, &t);
	}

	cout << "computed " << t << endl;

	if ( t > 3749 ) {
		assert(t >  303726787389ULL);
		assert(t == 303766880536ULL);
	}

	return t;
}

solveResult day7Solver::compute2() {
	s_part2 = true;
	solveResult t = 0;

	for ( const auto& line : m_data ) {
		computeLine(line, &t);
	}

	cout << "computed2 " << t << endl;

	if (t > 11387)
		assert(t == 337041851384440ULL);

    return t;
}

void day7Solver::loadTestData() {
  clearData();
    loadData("190: 10 19");
	loadData("3267: 81 40 27");
	loadData("83 : 17 5");
	loadData("156 : 15 6");
	loadData("7290 : 6 8 6 15");
	loadData("161011 : 16 10 13");
	loadData("192 : 17 8 14");
	loadData("21037 : 9 7 18 13");
	loadData("292 : 11 6 16 20");
}
