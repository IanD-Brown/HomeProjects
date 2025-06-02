#include "day16Solver.h"

#include "grid.h"

using namespace std;

using CostKey = pair<size_t, bool>;
using MazeCostMap = map<CostKey, solveResult>;

static CostKey makeCostKey(size_t c, Direction d) {
	return make_pair(c, d == EAST || d == WEST);
}

static solveResult getMoveCost(const map<CostKey, solveResult> &cellCost, const CostKey &key) {
	map<CostKey, solveResult>::const_iterator fnd(cellCost.find(key));
	solveResult stepCost(LLONG_MAX);
	if (fnd != cellCost.cend()) {
		stepCost = fnd->second + 1LL;
	}
	fnd = cellCost.find(make_pair(key.first, !key.second));
	if (fnd != cellCost.cend()) {
		stepCost = min(stepCost, fnd->second + 1001LL);
	}
	return stepCost;
}


struct MazeGrid : public Grid<CostKey> {
	size_t m_end;
	solveResult m_pathTotal;

	MazeGrid(const vector<string> &data) :
		Grid(data, &makeCostKey, &getMoveCost),
		m_end(0),
		m_pathTotal(0) {}

	solveResult getCellCost(const CostKey &k) {
		return getCellCost(k.first, k.second);
	}

	solveResult getCost(size_t cell, int verticalCost) const;

	solveResult countCellsInBestPath(size_t end);

	string toString(size_t cell) const;

	solveResult getCellCost(size_t of, bool horizontal) const;

	void bestPathRecurse(set<Step> &bestPathCells, vector<Step> &from, solveResult pathCost) const;
};

solveResult MazeGrid::countCellsInBestPath(size_t end) {
	m_end = end;
	m_pathTotal = getCost(m_end, 0);
	set<Step> bestPathCells;
	vector<Step> pathCells({make_pair(m_start, EAST)});
	bestPathRecurse(bestPathCells, pathCells, 0LL);
	bestPathCells.insert(make_pair(m_end, NONE));

	set<size_t> cellIndices;
	for (const Step &s : bestPathCells) {
		cellIndices.insert(s.first);
	}

	return cellIndices.size();
}

void MazeGrid::bestPathRecurse(set<Step> &bestPathCells,
										vector<Step> &from,
										solveResult pathCost) const {
	Direction blocked(opposite(from.back().second));
	size_t fromCell(from.back().first);
	solveResult trueCost(getCellCost(fromCell, true));
	solveResult falseCost(getCellCost(fromCell, false));
	for (const Direction &d : ALL_DIRECTIONS) {
		if (d != blocked && isPath(fromCell, d)) {
			size_t nextCell(move(fromCell, d));
			Step nextStep(make_pair(nextCell, d));
			if (find(from.cbegin(), from.cend(), nextStep) == from.cend()) {
				solveResult add(false);
				if (fromCell == m_start) {
					if (getCellCost(nextCell, true) == trueCost + 1LL) {
						add = true;
					} else if (getCellCost(nextCell, false) == trueCost + 1001LL) {
						add = true;
					}
				} else {
					if (getCellCost(nextCell, true) == trueCost + 1LL ||
						getCellCost(nextCell, false) == falseCost + 1LL) {
						add = true;
					} else if (getCellCost(nextCell, false) == trueCost + 1001LL ||
							   getCellCost(nextCell, true) == falseCost + 1001LL) {
						add = true;
					}
				}
				if (add > 0) {
					solveResult addition(d == from.back().second ? 1LL : 1001LL);
					if (nextCell == m_end) {
						if (pathCost + addition == m_pathTotal) {
							for (const Step &f : from) {
								bestPathCells.insert(f);
							}
						}
						return;
					}
					if (pathCost + addition < m_pathTotal) {
						from.push_back(nextStep);
						bestPathRecurse(bestPathCells, from, pathCost + addition);
						from.pop_back();
					}
				}
			}
		}
	}
}

solveResult MazeGrid::getCellCost(size_t cell, bool horizontal) const {
	MazeCostMap::const_iterator fnd(m_cellCost.find(make_pair(cell, horizontal)));

	return fnd != m_cellCost.cend() ? fnd->second : LLONG_MAX;
}

solveResult MazeGrid::getCost(size_t cell, int verticalCost) const {
	MazeCostMap::const_iterator fndTrue(m_cellCost.find(make_pair(cell, true)));
	MazeCostMap::const_iterator fndFalse(m_cellCost.find(make_pair(cell, false)));

	if (fndTrue != m_cellCost.cend()) {
		if (fndFalse != m_cellCost.cend()) {
			return min(fndTrue->second, fndFalse->second + verticalCost);
		}
		return fndTrue->second;
	}

	return fndFalse != m_cellCost.cend() ? fndFalse->second + verticalCost : LLONG_MAX;
}

string MazeGrid::toString(size_t cell) const {
	MazeCostMap::const_iterator fndTrue(m_cellCost.find(make_pair(cell, true)));
	MazeCostMap::const_iterator fndFalse(m_cellCost.find(make_pair(cell, false)));

	if (fndTrue != m_cellCost.cend()) {
		if (fndFalse != m_cellCost.cend()) {
			return to_string(fndTrue->second) + ";" + to_string(fndFalse->second);
		}
		return to_string(fndTrue->second);
	}

	if (fndFalse != m_cellCost.cend()) {
		return to_string(fndFalse->second);
	}
	return "";
}

day16Solver::day16Solver(const string &testFile) :
	solver(testFile) {}

void day16Solver::loadData(const string &line) { 
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

void day16Solver::clearData() { 
	m_data.clear();
}

solveResult day16Solver::compute() {
	MazeGrid grid(m_data);
	bool fromStart(true);
	if (fromStart || !m_part1) {
		grid.calc(grid.index(COORD_ROW(m_start), COORD_COL(m_start)), false);
		size_t end(grid.index(COORD_ROW(m_end), COORD_COL(m_end)));
		return m_part1 ? grid.getCost(end, 0) : grid.countCellsInBestPath(end);
	} else {
		grid.calc(grid.index(COORD_ROW(m_end), COORD_COL(m_end)), true);
		return grid.getCost(grid.index(COORD_ROW(m_start), COORD_COL(m_start)), 1000);
	}
}

void day16Solver::loadTestData() {
	bool state(m_part1);
	clearData();

	loadData("#####");
	loadData("#..E#");
	loadData("#.###");
	loadData("#...#");
	loadData("###.#");
	loadData("#S..#");
	loadData("#####");

	m_part1 = false;
	assertEquals(compute(), m_part1 ? 4010 : 11, "Small one");

	loadData("#################");
	loadData("#...#...#...#..E#");
	loadData("#.#.#.#.#.#.#.#.#");
	loadData("#.#.#.#...#...#.#");
	loadData("#.#.#.#.###.#.#.#");
	loadData("#...#.#.#.....#.#");
	loadData("#.#.#.#.#.#####.#");
	loadData("#.#...#.#.#.....#");
	loadData("#.#.#####.#.###.#");
	loadData("#.#.#.......#...#");
	loadData("#.#.###.#####.###");
	loadData("#.#.#...#.....#.#");
	loadData("#.#.#.#####.###.#");
	loadData("#.#.#.........#.#");
	loadData("#.#.#.#########.#");
	loadData("#S#.............#");
	loadData("#################");

	m_part1 = false;
	assertEquals(compute(), m_part1 ? 11048 : 64, "Second case");

	m_part1 = state;
	loadData("###############");
	loadData("#.......#....E#");
	loadData("#.#.###.#.###.#");
	loadData("#.....#.#...#.#");
	loadData("#.###.#####.#.#");
	loadData("#.#.#.......#.#");
	loadData("#.#.#####.###.#");
	loadData("#...........#.#");
	loadData("###.#.#####.#.#");
	loadData("#...#.....#.#.#");
	loadData("#.#.#.###.#.#.#");
	loadData("#.....#...#.#.#");
	loadData("#.###.#.#.#.#.#");
	loadData("#S..#.....#...#");
	loadData("###############");
}
