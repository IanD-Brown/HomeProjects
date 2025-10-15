#include "day20Solver.h"

#include <regex>

#include "grid.h"

using namespace std;
using CostMap = map<size_t, solveResult>;

day20Solver::day20Solver(const string &testFile):
	solver(testFile) {}

void day20Solver::loadData(const string &line) {
	regex allWall("^#+$");
	if (line.empty() || regex_match(line, allWall)) {
		return;
	}
	if (line[0] != '#' || line[line.length() - 1] != '#') {
		exit(666);
	}
	string noWall(line.substr(1, line.length() - 2));
	size_t pos(noWall.find('S'));
	if (pos != string::npos) {
		m_start = make_pair(m_data.size(), pos);
	} else {
		pos = noWall.find('E');
		if (pos != string::npos) {
			m_end = make_pair(m_data.size(), pos);
		}
	}
	m_data.push_back(noWall);
}

void day20Solver::clearData() {
	m_data.clear();
}

struct CheatSaving {
	Grid<size_t> m_grid;
	int m_maxLen;
	set<pair<size_t, size_t>> m_savings;
	solveResult m_baseCost;
	CostMap m_forwardCost;

	CheatSaving(const vector<string> &data, int maxLen) :
		m_baseCost(0),
		m_maxLen(maxLen),
		m_grid(data,
		[](size_t c, Direction d) {return c; },
		[](const map<size_t, solveResult> &costMap, const size_t key) {
			auto fnd(costMap.find(key));
			return fnd != costMap.cend() ? fnd->second + 1LL : LLONG_MAX;
		}) {
	}

	int rowCheatStart(int r) const {
		return max(r - m_maxLen, 0);
	}

	int rowCheatEnd(int r) const {
		return min(r + m_maxLen + 1, (int)m_grid.m_rowCount);
	}

	int colCheatStart(int c) const {
		return max(c - m_maxLen, 0);
	}

	int colCheatEnd(int c) const {
		return min(c + m_maxLen + 1, (int)m_grid.m_colCount);
	}

	int cheatCost(int r1, int r2, int c1, int c2) const { 
		int amount(abs(r1 - r2) + abs(c1 - c2));
		return amount < 2 || amount > m_maxLen ? -1 : amount;
	}

	int asRavel(int r, int c) const { 
		return r * m_grid.m_colCount + c;
	}

	size_t cheatCount(const coordinate& start, const coordinate& end, int minCheat) {
		m_savings.clear();
		size_t startIndex(m_grid.index(start.first, start.second));
		m_grid.calc(startIndex, false);
		m_forwardCost = m_grid.m_cellCost;
		size_t endIndex(m_grid.index(end.first, end.second));
		m_grid.calc(endIndex, false);
		m_baseCost = m_forwardCost[endIndex] - minCheat;

		for (int rowStart = 0; rowStart < m_grid.m_rowCount; ++rowStart) {
			int rce(rowCheatEnd(rowStart));
			for (int colStart = 0; colStart < m_grid.m_colCount; ++colStart) {
				if (!m_grid.isPath(rowStart, colStart, NONE)) {
					continue; // cheat always starts in a path
				}
				size_t cheatStart(m_grid.index(rowStart, colStart));
				for (int rowEnd = rowCheatStart(rowStart); rowEnd < rce; ++rowEnd) {
					for (int colEnd = colCheatStart(colStart); colEnd < colCheatEnd(colStart); ++colEnd) {
						int cheatCost(cheatCost(rowStart, rowEnd, colStart, colEnd));

						if (cheatCost < 0) {
							continue;
						}

						solveResult startCost(m_forwardCost[cheatStart] + cheatCost);
						if (startCost >= m_baseCost) {
							continue;
						}
						size_t cheatEnd(m_grid.index(rowEnd, colEnd));
						if (m_grid.isPath(cheatEnd, NONE) &&
							startCost + m_grid.m_cellCost[cheatEnd] < m_baseCost) {
							m_savings.insert(make_pair(cheatStart, cheatEnd));
						}
					}
				}
			}
		}

		return m_savings.size();
	}
};

solveResult day20Solver::compute() {
	CheatSaving cheatSaving(m_data, m_part1 ? 2 : 20);

	int minCheat(99);

	if (m_test) {
		minCheat = m_part1 ? 0 : 49;
	}

	return cheatSaving.cheatCount(m_start, m_end, minCheat);
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
