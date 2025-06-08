#include "day21Solver.h"

#include <regex>

#include "grid.h"

using namespace std;
using CostMap = map<size_t, solveResult>;

day21Solver::day21Solver(const string &testFile):
	solver(testFile) {}

void day21Solver::loadData(const string &line) {
	m_data.push_back(line);
}

void day21Solver::clearData() {
	m_data.clear();
}

solveResult day21Solver::compute() { return 0; }

void day21Solver::loadTestData() {
	m_test = true;

	clearData();

	loadData("029A");
	loadData("980A");
	loadData("179A");
	loadData("456A");
	loadData("379A");
}
