#include "day16Solver.h"

#include <climits>
#include <iomanip>
#include <iostream>
#include <functional>
#include <map>
#include <sstream>

using namespace std;

static const char WALL('#');

enum Direction { NORTH, SOUTH, EAST, WEST };

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
}

static coordinate move(const coordinate &from, const Direction &direction) {
	switch(direction) {
	case NORTH:
		return make_pair(COORD_ROW(from) - 1,COORD_COL(from));
	case SOUTH:
		return make_pair(COORD_ROW(from) + 1,COORD_COL(from));
	case EAST:
		return make_pair(COORD_ROW(from),COORD_COL(from) - 1);
	case WEST:
		return make_pair(COORD_ROW(from),COORD_COL(from) + 1);
	}
	return make_pair(0,0);
}

struct CellCost {
	bool m_endCell;
	solveResult m_baseCost;
	set<Direction> m_baseDirections;
	bool m_inBestPath;

	CellCost() : m_endCell(false), m_baseCost(LLONG_MAX), m_inBestPath(false) {}
	
	bool adjustBaseCost(solveResult cost, Direction direction) {
		if (!m_endCell && cost < m_baseCost) {
			m_baseCost = cost;
			m_baseDirections.clear();
			m_baseDirections.insert(direction);
			return true;
		} else if (cost == m_baseCost) {
			m_baseDirections.insert(direction);
		}
		return false;
	}

	solveResult getCost(const Direction &entry) const {
		if (m_endCell) {
			return 1;
		}
		return m_baseCost +
			   (m_baseDirections.find(entry) != m_baseDirections.end() ? 1LL : 1001LL);
	}

	static friend ostream &operator<<(ostream &os, CellCost const &cellCost) {
		if (cellCost.m_endCell) {
			os << "end cell";
		} else {
			os << cellCost.m_baseCost << " exiting ";
			for (const Direction &e : cellCost.m_baseDirections) {
				os << ToString(e) << ',';
			}
		}
		
		return os;
	}
};

struct PathFinder {
	const coordinate &m_start;
	const coordinate &m_end;
	map<coordinate, CellCost> m_cellCosts;
	const vector<string> &m_data;

	PathFinder(const day16Solver &problem) :
		m_start(problem.m_start), m_end(problem.m_end), m_data(problem.m_data) {
		for (size_t r = 0; r < problem.m_data.size(); ++r) {
			const string &line(problem.m_data[r]);
			for (size_t c = 0; c < line.size(); ++c) {
				if (line[c] != WALL) {
					m_cellCosts[make_pair(r, c)] = {};
				}
			}
		}
		m_cellCosts.find(m_end)->second.m_endCell = true;
	}

	bool isPath(const coordinate &from, const Direction &move) const {
		switch (move) {
		case NORTH:
			return m_data[COORD_ROW(from) - 1][COORD_COL(from)] != WALL;
		case SOUTH:
			return m_data[COORD_ROW(from) + 1][COORD_COL(from)] != WALL;
		case EAST:
			return m_data[COORD_ROW(from)][COORD_COL(from) - 1] != WALL;
		case WEST:
			return m_data[COORD_ROW(from)][COORD_COL(from) + 1] != WALL;
		}
		return false;
	}

	solveResult calc() {
		set<coordinate> processing({m_end});

		for (; !processing.empty();) {
			set<coordinate> followers;
			for (const auto &p : processing) {
				calc(p, [&](coordinate c) { followers.insert(c); });
			}
			processing.clear();
			processing.insert(followers.begin(), followers.end());
		}
		//for (const auto &t : m_cellCosts) {
		//	cout << t.first.first << '/' << t.first.second << ' ' << t.second
		//		 << endl;
		//}
		const auto &startCost(m_cellCosts[m_start]);
		return startCost.m_baseCost +
			   (startCost.m_baseDirections.find(EAST) != startCost.m_baseDirections.cend() ? 0 : 1000LL);
	}

	void calc(const coordinate &from, function<void(coordinate)> follow);

	solveResult countCellsInBestPath();

	solveResult markBestPathCells(const coordinate &from, const set<Direction> &directions);
};

void PathFinder::calc(const coordinate &from, function<void(coordinate)> follow) {
	const map<coordinate, CellCost>::iterator fromCost(m_cellCosts.find(from));

	for (const auto &fromEntry : ALL_DIRECTIONS) {
		if (isPath(from, fromEntry)) {
			coordinate entryCell(move(from, fromEntry));
			const map<coordinate, CellCost>::iterator entryCellCost(m_cellCosts.find(entryCell));

			if (entryCellCost->second.adjustBaseCost(fromCost->second.getCost(fromEntry), fromEntry)) {
				follow(entryCell);
			}
		}
	}
}

solveResult PathFinder::countCellsInBestPath() {
	const map<coordinate, CellCost>::iterator startCost(m_cellCosts.find(m_start));
	startCost->second.m_inBestPath = true;
	return markBestPathCells(m_start, startCost->second.m_baseDirections);
}

solveResult PathFinder::markBestPathCells(const coordinate &from, const set<Direction> &directions) {
	solveResult result(0);

	for (const Direction &direction : directions) {
		coordinate next(move(from, opposite(direction)));
		const map<coordinate, CellCost>::iterator nextCost(m_cellCosts.find(next));

		if (!nextCost->second.m_inBestPath) {
			nextCost->second.m_inBestPath = true;
			result += 1 + markBestPathCells(next, nextCost->second.m_baseDirections);
		}
	}

	if (from == m_start) {
		vector<string> now(m_data);
		solveResult count(0);
		for (const auto f : m_cellCosts) {
			if (f.second.m_inBestPath) {
				now[COORD_ROW(f.first)][COORD_COL(f.first)] = '0';
				++count;
			}
		}
		for (const auto l : now) {
			cout << l << endl;
		}
		cout << count << endl;
		int r(0);
		int c(0);
		for (const auto f : m_cellCosts) {
			int cellRow(COORD_ROW(f.first));
			if (cellRow > r) {
				cout << endl;
				r = cellRow;
				c = 0;
			}
			int cellCol(COORD_COL(f.first));
			for (; c + 1 < cellCol; ++c) {
				cout << "    ,";
			}
			c = cellCol;

			cout << setw(4) << (f.second.m_endCell ? 0 : f.second.m_baseCost) << ',';
		}
		cout << endl;
	}
	return result;
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
	PathFinder pathFinder(*this);
	if (m_part1) {
		return pathFinder.calc();
	} else {
		pathFinder.calc();
		return pathFinder.countCellsInBestPath();
	}
}

void day16Solver::loadTestData() {
	clearData();

	loadData("#####");
	loadData("#..E#");
	loadData("#.###");
	loadData("#...#");
	loadData("###.#");
	loadData("#S..#");
	loadData("#####");

	assertEquals(compute(), m_part1 ? 4010 : 11, "Small one");

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
