#include "day16Solver.h"

#include <map>

enum Direction { NORTH, SOUTH, EAST, WEST, NONE };

using namespace std;

using Step = pair<size_t, Direction>;
using CostMap = map<pair<size_t, bool>, solveResult>;

static const char WALL('#');

static const Direction ALL_DIRECTIONS[] = {NORTH, SOUTH, EAST, WEST};

static const char *ToString(const Direction &v) {
	switch(v) {
	case NORTH:
		return "N";
	case SOUTH:
		return "S";
	case EAST:
		return "E";
	case WEST:
		return "W";
	}
	return NULL;
}

static Direction opposite(const Direction &from) {
	switch (from) {
	case NORTH:
		return SOUTH;
	case SOUTH:
		return NORTH;
	case EAST:
		return WEST;
	case WEST:
		return EAST;
	}
	return NONE;
}

struct Grid {
	const vector<string>& m_data;
	const size_t m_rowCount;
	const size_t m_colCount;

	Grid(const vector<string> &data) : m_data(data), m_rowCount(data.size()), m_colCount(data[0].size()) {}

	size_t index(size_t row, size_t col) const {
		return row * m_colCount + col;
	}

	size_t size() const { 
		return m_rowCount * m_colCount;
	}

	size_t row(size_t ravelIndex) const {
		return (ravelIndex - col(ravelIndex)) / m_colCount;
	}

	size_t col(size_t ravelIndex) const {
		return ravelIndex % m_colCount;
	}
};

struct MazeGrid : public Grid {
	CostMap m_cellCost;
	size_t m_start;
	size_t m_end;
	solveResult m_pathTotal;

	MazeGrid(const vector<string> &data);

	void calc(size_t start, bool isEnd);

	void calc(set<Step> &next, Step from);

	size_t move(size_t from, const Direction &direction) const;

	bool isPath(size_t row, size_t col, Direction move) const;

	bool isPath(size_t from, Direction move) const;

	solveResult getCost(size_t cell, int verticalCost) const;

	solveResult countCellsInBestPath(size_t end);

	string toString(size_t cell) const;

	solveResult getMoveCost(size_t from, bool horizontal) const;

	solveResult getCellCost(size_t of, bool horizontal) const;

	void bestPathRecurse(set<Step> &bestPathCells, vector<Step> &from, solveResult pathCost) const;
};

MazeGrid::MazeGrid(const vector<string> &data) : Grid(data), m_start(0), m_end(0), m_pathTotal(0) {
}

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

void MazeGrid::bestPathRecurse(set<Step> &bestPathCells, vector<Step> &from, solveResult pathCost) const {
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

solveResult MazeGrid::getCellCost(size_t cell,bool horizontal) const {
	CostMap::const_iterator fnd(m_cellCost.find(make_pair(cell, horizontal)));

	return fnd != m_cellCost.cend() ? fnd->second : LLONG_MAX;
}

solveResult MazeGrid::getCost(size_t cell, int verticalCost) const {
	CostMap::const_iterator fndTrue(m_cellCost.find(make_pair(cell, true)));
	CostMap::const_iterator fndFalse(m_cellCost.find(make_pair(cell, false)));

	if (fndTrue != m_cellCost.cend()) {
		if (fndFalse != m_cellCost.cend()) {
			return min(fndTrue->second, fndFalse->second + verticalCost);
		}
		return fndTrue->second;
	}

	return fndFalse != m_cellCost.cend() ? fndFalse->second + verticalCost : LLONG_MAX;
}

string MazeGrid::toString(size_t cell) const {
	CostMap::const_iterator fndTrue(m_cellCost.find(make_pair(cell, true)));
	CostMap::const_iterator fndFalse(m_cellCost.find(make_pair(cell, false)));

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

void MazeGrid::calc(size_t start, bool isEnd) {
	m_start = start;
	m_cellCost[make_pair(start, true)] = 0;
	if (isEnd)
		m_cellCost[make_pair(start, false)] = 0;

	set<Step> processing({make_pair(start, NONE)});

	while (!processing.empty()) {
		set<Step> next;
		for (const Step &p : processing) {
			calc(next, p);
		}
		processing = next;
	}
}

void MazeGrid::calc(set<Step> &next, Step from) {
	Direction blocked(opposite(from.second));
	for (const Direction &d : ALL_DIRECTIONS) {
		if (d != blocked && isPath(from.first, d)) {
			bool horizontal(d == EAST || d == WEST);
			solveResult stepCost(getMoveCost(from.first, horizontal));
			
			pair<size_t, bool> nextCostKey(
				make_pair(move(from.first, d), horizontal));
			CostMap::iterator nextCost(m_cellCost.find(nextCostKey));
			if (nextCost != m_cellCost.end()) {
				// existing
				if (nextCost->second > stepCost) {
					nextCost->second = stepCost;
					next.insert(make_pair(nextCostKey.first, d));
				}
			} else {
				m_cellCost[nextCostKey] = stepCost;
				next.insert(make_pair(nextCostKey.first, d));
			}
		}
	}
}

solveResult MazeGrid::getMoveCost(size_t from, bool horizontal) const {
	CostMap::const_iterator fnd(m_cellCost.find(make_pair(from, horizontal)));
	solveResult stepCost(LLONG_MAX);
	if (fnd != m_cellCost.cend()) {
		stepCost = fnd->second + 1LL;
	}
	fnd = m_cellCost.find(make_pair(from, !horizontal));
	if (fnd != m_cellCost.cend()) {
		stepCost = min(stepCost, fnd->second + 1001LL);
	}
	return stepCost;
}

size_t MazeGrid::move(size_t from, const Direction &direction) const {
		switch (direction) {
		case NORTH:
			return index(row(from) - 1, col(from));
		case SOUTH:
			return index(row(from) + 1, col(from));
		case EAST:
			return index(row(from), col(from) + 1);
		case WEST:
			return index(row(from), col(from) - 1);
		}
		return from;
	}

bool MazeGrid::isPath(size_t row, size_t col, const Direction move) const {
	switch (move) {
	case NORTH:
		return row > 0 && m_data[row - 1][col] != WALL;
	case SOUTH:
		return m_data[row + 1][col] != WALL;
	case EAST:
		return m_data[row][col + 1] != WALL;
	case WEST:
		return col > 0 && m_data[row][col - 1] != WALL;
	}
	return m_data[row][col] != WALL;
}

bool MazeGrid::isPath(size_t from, const Direction move) const {
	return isPath(row(from), col(from), move);
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
