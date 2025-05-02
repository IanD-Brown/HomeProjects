#include <cassert>
#include <iostream>

#include "day12Solver.h"

using namespace std;

static bool s_part1 = true;

enum Direction { UP, DOWN, LEFT, RIGHT };

static const char* ToString(const Direction& v) {
	switch ( v ) {
	case UP: return "Up";
	case DOWN: return "Down";
	case LEFT: return "Left";
	case RIGHT:return "Right";
	}
	return NULL;
}

struct WallWalker {
	size_t m_turnCount;
	const day12Solver::Region& m_region;
	size_t m_r;
	size_t m_cIndex;
	Direction m_heading;
	map<size_t, vector<size_t>> m_bottomEdges;

	WallWalker(const day12Solver::Region& region) : m_turnCount(0), m_region(region), m_r(0), m_cIndex(0), m_heading(RIGHT) {}

	size_t countCorners(size_t startRow, size_t colIndex) {
		m_heading = RIGHT;
		followOuterEdge(startRow, colIndex);

		while ( !m_bottomEdges.empty() ) {
			const auto& edge = m_bottomEdges.cbegin();
			size_t column = edge->second[ 0 ];
			const auto& iter = find(m_region.m_cells[edge->first].cbegin(), m_region.m_cells[ edge->first ].cend(), column);
			size_t bottomColIndex = distance(m_region.m_cells[ edge->first ].cbegin(), iter);
			m_heading = RIGHT;
			followRightEdge(edge->first, bottomColIndex);
		}
		for ( const auto& m : m_bottomEdges ) {
			if ( !m.second.empty() ) {
				for ( auto c : m.second ) {
					cout << "remaining bottom " << m.first << '/' << c << endl;
				}
			}
		}
		return m_turnCount;
	}

	void addBottomEdge(size_t r, size_t c) {
		auto fnd = m_bottomEdges.find(r);

		if ( fnd == m_bottomEdges.end() ) {
			m_bottomEdges[ r ] = { c };\
		} else {
			fnd->second.push_back(c);
		}
	}

	void removeBottom(size_t r, size_t c) {
		auto fnd = m_bottomEdges.find(r);

		if ( fnd != m_bottomEdges.end() ) {
			auto iter = find(fnd->second.begin(), fnd->second.end(), c);
			if ( iter != fnd->second.end() ) {
				fnd->second.erase(iter);
			} else {
				cout << "missing bottom " << r << '/' << c << endl;
			}
		}
		else {
			cout << "row missing " << r << '/' << c << endl;
		}
		fnd = m_bottomEdges.find(r);
		if ( fnd != m_bottomEdges.end() && fnd->second.empty() ) {
			m_bottomEdges.erase(fnd);
		}

	}

	void changeRow(int rowAdjust, size_t columnValue) {
		m_r += rowAdjust;
		m_cIndex = distance(m_region.m_cells[ m_r ].begin(), m_region.m_cells[ m_r ].find(columnValue));
	}

	void turnRow(int turnCount, Direction heading, int rowAdjust, size_t columnValue) {
		m_turnCount += turnCount;
		m_heading = heading;
		changeRow(rowAdjust, columnValue);
	}

	void turnInColumn(int turnCount, Direction heading, int columnAdjust) {
		m_turnCount += turnCount;
		m_heading = heading;
		m_cIndex += columnAdjust;
	}

	void followOuterEdge(size_t startRow, size_t colIndex);

	void followRightEdge(size_t startRow, size_t colIndex);
};

void WallWalker::followRightEdge(size_t startRow, size_t colIndex) {
	m_r = startRow;
	m_cIndex = colIndex;
	size_t limit = m_region.cellCount() * 10;
	for (size_t count = 0 ; count < limit; ++count) {
		size_t c = *next(m_region.m_cells[ m_r ].begin(), m_cIndex);
		switch ( m_heading ) {
		case RIGHT:
			if ( m_region.hasCellBelow(m_r, c) ) {
				turnRow(1, DOWN, 1, c);
			} else if ( m_region.hasCellAfter(m_r, c) ) {
				removeBottom(m_r, c);
				++m_cIndex;
			} else if ( m_region.hasCellAbove(m_r, c) ) {
				removeBottom(m_r, c);
				turnRow(1, UP, -1, c);
			} else if ( m_region.hasCellBefore(m_r, c) ) {
				turnInColumn(2, RIGHT, -1);
			} else {
				removeBottom(m_r, c);
				m_turnCount += 4;
				return;
			}
			break;
		case DOWN:
			if ( m_region.hasCellBefore(m_r, c) ) {
				turnInColumn(1, LEFT, -1);
			} else if ( m_region.hasCellBelow(m_r, c) ) {
				changeRow(1, c);
			} else if ( m_region.hasCellAfter(m_r, c) ) {
				turnInColumn(1, RIGHT, 1);
			} else {
				turnRow(2, UP, -1, c);
			}
			break;
		case LEFT:
			if ( m_region.hasCellAbove(m_r, c) ) {
				turnRow(1, UP, -1, c);
			} else if ( m_region.hasCellBefore(m_r, c) ) {
				removeBottom(m_r, c);
				--m_cIndex;
			} else if (m_region.hasCellBelow(m_r, c)) {
				removeBottom(m_r, c);
				turnRow(1, DOWN, 1, c);
			} else  {
				turnInColumn(2, RIGHT, 1);
			}
			break;
		case UP:
			if ( m_region.hasCellAfter(m_r, c) ) {
				turnInColumn(1, RIGHT, 1);
			} else if ( m_region.hasCellAbove(m_r, c) ) {
				changeRow(-1, c);
			} else if ( m_region.hasCellBefore(m_r, c) ) {
				turnInColumn(1, LEFT, -1);
			} else  {
				turnRow(2, DOWN, 1, c);
			}
			break;
		}
		if ( m_r == startRow && m_cIndex == colIndex ) {
			m_turnCount += ( m_heading == LEFT ? 2 : 0 );
			return;
		}
	}
	cout << "Over the limit " << limit << endl;
}

void WallWalker::followOuterEdge(size_t startRow, size_t colIndex) {
	m_r = startRow;
	m_cIndex = colIndex;
	size_t c = *next(m_region.m_cells[ m_r ].begin(), m_cIndex);
	assert(m_region.hasCellAbove(startRow, c) == false);
	assert(m_region.hasCellBefore(startRow, c) == false);
	bool hasBelow = m_region.hasCellBelow(m_r, c);
	for ( ;; ) {
		 c = *next(m_region.m_cells[ m_r ].begin(), m_cIndex);
		switch ( m_heading ) {
		case RIGHT:
			if ( m_region.hasCellAbove(m_r, c) ) {
				turnRow(1, UP, -1, c);
			} else if ( m_region.hasCellAfter(m_r, c) ) {
				++m_cIndex;
			} else if ( m_region.hasCellBelow(m_r, c) ) {
				turnRow(1, DOWN, 1, c);
			} else if ( m_region.hasCellBefore(m_r, c) ) {
				removeBottom(m_r, c);
				turnInColumn(2, LEFT, -1);
			} else {
				removeBottom(m_r, c);
				m_turnCount += 4;
				return;
			}
			break;
		case DOWN:
			if ( m_region.hasCellAfter(m_r, c) ) {
				turnInColumn(1, RIGHT, 1);
			} else if ( m_region.hasCellBelow(m_r, c) ) {
				changeRow(1, c);
			} else if ( m_region.hasCellBefore(m_r, c) ) {
				removeBottom(m_r, c);
				turnInColumn(1, LEFT, -1);
			} else {
				removeBottom(m_r, c);
				turnRow(2, UP, -1, c);
			}
			break;
		case LEFT:
			removeBottom(m_r, c);
			if ( m_region.hasCellBelow(m_r, c) ) {
				turnRow(1, DOWN, 1, c);
			} else if ( m_region.hasCellBefore(m_r, c) ) {
				--m_cIndex;
			} else if ( m_region.hasCellAbove(m_r, c) ) {
				turnRow(1, UP, -1, c);
			} else {
				turnInColumn(2, RIGHT, 1);
			}
			break;
		case UP:
			if ( m_region.hasCellBefore(m_r, c) ) {
				turnInColumn(1, LEFT, -1);
			} else if ( m_region.hasCellAbove(m_r, c) ) {
				changeRow(-1, c);
			} else if ( m_region.hasCellAfter(m_r, c) ) {
				turnInColumn(1, RIGHT, 1);
			} else {
				turnRow(2, DOWN, 1, c);
			}
			break;
		}
		if ( m_r == startRow && m_cIndex == colIndex ) {
			if ( hasBelow && m_heading == LEFT) {
				hasBelow = false;
			} else {
				if ( m_heading == LEFT ) {
					removeBottom(m_r, *next(m_region.m_cells[ m_r ].begin(), m_cIndex));
					m_turnCount += 2;
				} else {
					++m_turnCount;
				}
				return;
			}
		}
	}

}

day12Solver::plantArea::plantArea(size_t r, size_t c) {
	add(r, c);
}

void day12Solver::plantArea::add(size_t r, size_t c) {
	while ( m_locations.size() <= r ) {
		m_locations.emplace_back();
	}
	m_locations[ r ].push_back(c);
}

bool day12Solver::Region::addCell(size_t r, size_t c) {
	const size_t rowCount = m_cells.size();
	if ( rowCount > 0 ) {
		bool match = false;
		if ( rowCount > r ) {
			match = m_cells[ r ].find(c + 1) != m_cells[ r ].cend();
			if ( !match && c > 0 ) {
				match = m_cells[ r ].find(c - 1) != m_cells[ r ].cend();
			}
		}
		if ( !match && rowCount > r + 1 ) {
			match = m_cells[ r + 1 ].find(c) != m_cells[ r + 1 ].cend();
		}
		if ( !match && rowCount > r - 1 ) {
			match = m_cells[ r - 1 ].find(c) != m_cells[ r - 1 ].cend();
		}
		if ( !match ) {
			return false;
		}
	}

	while ( m_cells.size() <= r ) {
		m_cells.emplace_back();
	}
	m_cells[ r ].insert(c);
	return true;
}

size_t day12Solver::Region::cellCount() const {
	size_t cellCount = 0;
	for ( size_t r = 0; r < m_cells.size(); ++r ) {
		cellCount += m_cells[ r ].size();
	}
	return cellCount;
}

size_t day12Solver::Region::getCount() const {
	size_t cellCount = 0;
	size_t multiplier = 0;
	const size_t rowCount = m_cells.size();
	WallWalker walker(*this);
	size_t walkRow(0);

	for ( size_t r = 0; r < rowCount; ++r ) {
		cellCount += m_cells[ r ].size();

		for ( size_t c : m_cells[ r ] ) {
			if ( !hasCellAbove(r, c) ) {
				++multiplier;
			}
			if ( !hasCellBelow(r, c) ) {
				++multiplier;
				if ( !s_part1 ) {
					walker.addBottomEdge(r, c);
				}
			}
			if ( !hasCellBefore(r, c) ) {
				++multiplier;
			}
			if ( !hasCellAfter(r, c) ) {
				++multiplier;
			}
		}

		if ( !s_part1 && cellCount > 0 && cellCount == m_cells[ r ].size() ) {
			// counting corners.
			walkRow = r;
		}
	}
	if (!s_part1 ) {
		multiplier = walker.countCorners(walkRow, 0);
	}

	return cellCount * multiplier;
}

bool day12Solver::Region::hasCellBefore(size_t r, size_t c) const {
	return c > 0 && m_cells[ r ].find(c - 1) != m_cells[ r ].cend();
}

bool day12Solver::Region::hasCellAfter(size_t r, size_t c) const {
	return m_cells[ r ].find(c + 1) != m_cells[ r ].cend();
}

bool day12Solver::Region::hasCellBelow(size_t r, size_t c) const {
	return r + 1 < m_cells.size() && m_cells[ r + 1 ].find(c) != m_cells[ r + 1 ].cend();
}

bool day12Solver::Region::hasCellAbove(size_t r, size_t c) const {
	return r > 0 && m_cells[ r - 1 ].find(c) != m_cells[ r - 1 ].cend();
}

vector<day12Solver::Region> day12Solver::plantArea::getRegions(size_t rowCount, size_t colCount) {
	vector<vector<size_t>> toDo(m_locations);
	bool again = true;
	vector<Region> result;

	while ( again ) {
		day12Solver::Region region;
		again = false;
		bool matchFound = true;
		while ( matchFound ) {
			matchFound = false;
			for ( size_t r = 0; r < toDo.size(); ++r ) {
				vector<size_t> matched;
				for ( size_t i = 0; i < toDo[ r ].size(); ++i ) {
					if ( region.addCell(r, toDo[ r ][ i ]) ) {
						matched.push_back(i);
						matchFound = true;
					}
				}
				for ( int m = matched.size() - 1; m >= 0; --m ) {
					toDo[ r ].erase(toDo[ r ].begin() + matched[ m ]);
				}
			}
		}
		if ( !region.m_cells.empty() ) {
			result.push_back( region );
			again = true;
		}
	}
	return result;
}

day12Solver::day12Solver(const string& testFile) : solver(testFile), m_colCount(0), m_rowCount(0) {
}

void day12Solver::loadData(const string& line) {
	if ( !m_data.empty() ) {
		assert(line.length() == m_colCount);
	} else {
		m_colCount = line.length();
	}
	for ( size_t c = 0; c < line.length(); ++c ) {
		auto fnd = m_data.find(line[ c ]);

		if ( fnd == m_data.end() ) {
			m_data[ line[ c ] ] = new plantArea(m_rowCount, c);
		} else {
			fnd->second->add(m_rowCount, c);
		}
	}
	++m_rowCount;
}

void day12Solver::clearData() {
	for ( int i = 0; i < m_data.size(); ++i ) {
		delete m_data[ i ];
	}
	m_data.clear();
	m_colCount = 0;
	m_rowCount = 0;
}

solveResult day12Solver::compute() {
	solveResult t = 0;

	for ( auto& i : m_data ) {
		for ( const auto& r : i.second->getRegions(m_rowCount, m_colCount) ) {
			const size_t count = r.getCount();
			t += count;
		}
	}

	return t;
}

solveResult day12Solver::compute2() {
	s_part1 = false;
    return compute();
}

static void check(size_t actual, size_t expected, const string& message) {
	if ( actual != expected ) {
		cout << "actual " << actual << " expected " << expected << ' ' << message << endl;
		assert(actual == expected);
	}
}

void day12Solver::loadTestData() {
	bool state = s_part1;
	clearData();

	s_part1 = false;
	loadData("AAAA");
	loadData("AABA");
	loadData("ABAA");

	check(compute(), 128, "my data");
	clearData();

	loadData("AAAAAA");
	loadData("AAABBA");
	loadData("AAABBA");
	loadData("ABBAAA");
	loadData("ABBAAA");
	loadData("AAAAAA");

	check(compute(), 368, "inner region");
	clearData();

	loadData("MMMMMMMMMM");
	loadData("MMMMMAANNN");
	loadData("MMMMAANNNN");
	loadData("MAAAAAAANN");
	loadData("LLAAAAAAAN");

	check(compute(), 596, "does this loop"); // Might not be the correct value
	clearData();

	s_part1 = state;
	loadData("RRRRIICCFF");
	loadData("RRRRIICCCF");
	loadData("VVRRRCCFFF");
	loadData("VVRCCCJFFF");
	loadData("VVVVCJJCFE");
	loadData("VVIVCCJJEE");
	loadData("VVIIICJJEE");
	loadData("MIIIIIJJEE");
	loadData("MIIISIJEEE");
	loadData("MMMISSJEEE");
}
