#include "day18Solver.h"

#include <climits>
#include <iostream>

using namespace std;

struct MemoryGrid : Grid<size_t> {
	using CostMap = map<size_t, solveResult>;
	MemoryGrid(const vector<string> &data) :
		Grid(
		data, 
		[](size_t c, Direction d) { return c; },
		[](const CostMap &costMap, const size_t key) {
			auto fnd(costMap.find(key));
			return fnd != costMap.cend() ? fnd->second + 1LL : LLONG_MAX;
		}) {}

	solveResult getCellCost(size_t of) const {
		CostMap::const_iterator fnd(m_cellCost.find(of));

		return fnd != m_cellCost.cend() ? fnd->second : LLONG_MAX;
	}
};

day18Solver::day18Solver(const string &testFile) :
	solver(testFile), m_grid(0), m_limit(0), m_count(0) {}

void day18Solver::loadData(const string &line) {
	vector<int> point(asVectorInt(line, ","));
	if (point.size() == 2) {
		if (m_count++ < m_limit) {
			m_data[point[1]][point[0]] = WALL;
		} else if (!m_part1) {
			m_overflow.push_back(line);
		}
	}
}

void day18Solver::clearData() {
	m_data.clear();
	m_overflow.clear();
	if (m_grid != 0) {
		delete m_grid;
	}
	m_limit = m_test ? 12 : 1024;
	m_count = 0;
	size_t count(m_test ? 7 : 71);
	string emptyRow(count, '.');
	m_data = {count, emptyRow};
	m_grid = new MemoryGrid(m_data);
}

solveResult day18Solver::compute() {
	m_grid->calc(0, false);
	if (m_part1) {
		return m_grid->getCellCost(m_grid->size() - 1);
	}
	for (const string &l : m_overflow) {
		vector<int> point(asVectorInt(l, ","));
		m_data[point[1]][point[0]] = WALL;
		m_grid->calc(0, false);
		if (m_grid->getCellCost(m_grid->size() - 1) == LLONG_MAX) {
			cout << "No way... " << l << endl;

			return m_grid->index(point[1], point[0]);
		}
	}
	return -1;
}

void day18Solver::loadTestData() {
	m_test = true;

	clearData();

	loadData("5,4");
	loadData("4,2");
	loadData("4,5");
	loadData("3,0");
	loadData("2,1");
	loadData("6,3");
	loadData("2,4");
	loadData("1,5");
	loadData("0,6");
	loadData("3,3");
	loadData("2,6");
	loadData("5,1");
	loadData("1,2");
	loadData("5,5");
	loadData("2,5");
	loadData("6,5");
	loadData("1,4");
	loadData("0,4");
	loadData("6,4");
	loadData("1,1");
	loadData("6,1");
	loadData("1,0");
	loadData("0,5");
	loadData("1,6");
	loadData("2,0");
}
