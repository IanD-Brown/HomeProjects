#include "day19Solver.h"

#include <climits>
#include <iostream>
#include <map>
#include <set>

using namespace std;

day19Solver::day19Solver(const string &testFile) :
	solver(testFile) {}

void day19Solver::loadData(const string &line) {
}

void day19Solver::clearData() {
	m_data.clear();
}

solveResult day19Solver::compute() { return 0; }

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
