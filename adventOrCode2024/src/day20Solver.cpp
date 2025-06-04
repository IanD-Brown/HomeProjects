#include "day20Solver.h"

#include <cassert>
#include <iostream>

#include "grid.h"

using namespace std;
using CostMap = map<size_t, solveResult>;

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

static solveResult cost(const CostMap &costMap, size_t key) {
	auto fnd(costMap.find(key));
	return fnd != costMap.cend() ? fnd->second : LLONG_MAX;
}

struct CheatSaving {
	CostMap m_savings;
	solveResult m_baseCost;
	Grid<size_t> m_grid;
	CostMap m_forwardCost;
	size_t m_multiCheat;

	CheatSaving(const vector<string> &data) : m_multiCheat(0), 
		m_grid(data,
		[](size_t c, Direction d) {return c; },
		[](const map<size_t, solveResult> &costMap, const size_t key) {
		auto fnd(costMap.find(key));
		return fnd != costMap.cend() ? fnd->second + 1LL : LLONG_MAX;
		})  {
	}

	size_t cheatCount(const coordinate& start, const coordinate& end, int minCheat) {
		m_savings.clear();
		size_t startIndex(m_grid.index(start.first, start.second));
		m_grid.calc(startIndex, false);
		m_forwardCost = m_grid.m_cellCost;
		size_t endIndex(m_grid.index(end.first, end.second));
		m_grid.calc(endIndex, false);
		m_baseCost = m_forwardCost[endIndex] - minCheat;
		assert(m_forwardCost[endIndex] == m_grid.m_cellCost[startIndex]);

		for (int r = 0; r < m_grid.m_rowCount; ++r) {
			for (int c = 0; c < m_grid.m_colCount; ++c) {
				if (!m_grid.isPath(r, c, NONE)) {
					size_t index(m_grid.index(r, c));
					cheatCost(index);
				}
			}
		}

		return m_savings.size();
	}

	void cheatCost(size_t index) {
		set<size_t> pathCells;
		for (Direction d : ALL_DIRECTIONS) {
			if (m_grid.isPath(index, d)) {
				pathCells.insert(m_grid.move(index, d));
			}
		}
		for (size_t indexA : pathCells) {
			for (size_t indexB : pathCells) {
				if (indexA != indexB) {
					assert(cost(m_forwardCost, indexA) < LLONG_MAX);
					assert(cost(m_grid.m_cellCost, indexB) < LLONG_MAX);
					solveResult cheatCost(m_forwardCost[indexA] +
										  m_grid.m_cellCost[indexB] + 2LL);
					if (cheatCost < m_baseCost) {
						auto fnd(m_savings.find(index));

						if (fnd == m_savings.end()) {
							m_savings[index] = cheatCost;
						} else {
							++m_multiCheat;
							cout << "multiple cheat " << m_multiCheat << endl;

						}
					}
				}
			}
		}
	}
};

solveResult day20Solver::compute() {
	CheatSaving cheatSaving(m_data);

	return cheatSaving.cheatCount(m_start, m_end, m_test ? 0 : 99);
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
