#include "day20Solver.h"

#include <climits>
#include <iostream>
#include <map>
#include <set>

using namespace std;

day20Solver::day20Solver(const string &testFile):
	solver(testFile) {}

void day20Solver::loadData(const string &line) {
	size_t pos(line.find('S'));
	if (pos != string::npos) {
		m_start = make_pair(m_data.size(), pos);
	} else {
		pos = line.find('E');
		if (pos != string::npos) {
			m_end = make_pair(m_data.size(), pos);
		}
	}
	m_data.push_back(line);
}

void day20Solver::clearData() {
	m_data.clear();
}

solveResult day20Solver::compute() {
	solveResult count(0);
	return count;
}

void day20Solver::loadTestData() {
	m_test = true;

	clearData();

	loadData("###############");
	loadData("#...#...#.....#");
	loadData("#.#.#.#.#.###.#");
	loadData("#S#...#.#.#...#");
	loadData("#######.#.#.###");
	loadData("#######.#.#...#");
	loadData("#######.#.###.#");
	loadData("###..E#...#...#");
	loadData("###.#######.###");
	loadData("#...###...#...#");
	loadData("#.#####.#.###.#");
	loadData("#.#...#.#.#...#");
	loadData("#.#.#.#.#.#.###");
	loadData("#...#...#...###");
	loadData("###############");
}
