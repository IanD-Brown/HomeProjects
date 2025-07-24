#include "day22Solver.h"

using namespace std;

day22Solver::day22Solver(const string &testFile) :
	solver(testFile) {}

void day22Solver::loadData(const string &line) {
	m_data.push_back(line);
}

void day22Solver::clearData() {
	m_data.clear();
}

solveResult day22Solver::compute() {
	solveResult r(0);
	return r;
}

void day22Solver::loadTestData() {
	m_test = true;

	clearData();

	loadData("1");
	loadData("10");
	loadData("100");
	loadData("2024");
}
