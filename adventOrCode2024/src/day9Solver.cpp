#include <cassert>
#include <iostream>
#include <algorithm>

#include "day9Solver.h"

using namespace std;

static bool s_part2 = false;

struct Area {
	int m_fileId;
	vector<long> m_usedIndices;
	vector<long> m_freeIndices;

	Area(long firstBlock, int blockCount, int fileId) {
		m_fileId = fileId;
		for ( int c = 0; c < blockCount; ++c ) {
			if ( fileId >= 0 ) {
				m_usedIndices.push_back(firstBlock + c);
			} else {
				m_freeIndices.push_back(firstBlock + c);
			}
		}
	}

	long long checkSum() {
		long long r = 0;
		
		for ( long index : m_usedIndices ) {
			r += index * m_fileId;
		}
		return r;
	}

	bool canUseBlock(long blockIndex) {
		return m_fileId >= 0 && !m_usedIndices.empty() && m_usedIndices[ 0 ] > blockIndex;
	}
};

day9Solver::day9Solver(const string& testFile) : solver(testFile) {
}

void day9Solver::loadData(const string& line) {
	m_data += line;
}

void day9Solver::clearData() {
	m_data.clear();
}

static bool moveBlock(const vector<Area*>::reverse_iterator& limit, vector<Area*>::reverse_iterator& moveTo, long blockIndex) {
	while (moveTo != limit && !(*moveTo)->canUseBlock(blockIndex)) {
		++moveTo;
	}

	if ( moveTo != limit ) {
		for ( int i = ( *moveTo )->m_usedIndices.size() - 1; i >= 0; --i ) {
			if ( ( *moveTo )->m_usedIndices[ i ] > blockIndex ) {
				( *moveTo )->m_freeIndices.push_back(( *moveTo )->m_usedIndices[ i ]);
				(*moveTo )->m_usedIndices[ i ] = blockIndex;
				return true;
			}
		}
	}
	return false;
}

solveResult day9Solver::compute() {
	solveResult t = 0;
	bool isFree = false;
	int fileId = 0;
	long blockNumber = 0;
	vector<Area*> diskUse;

	for ( const auto& c : m_data ) {
		diskUse.push_back(new Area(blockNumber, c - '0', isFree ? -1 : fileId++));
		blockNumber += c - '0';
		isFree = !isFree;
	}

	if (!s_part2) {
		// Need to compact...
		vector<Area*>::reverse_iterator moveTo = diskUse.rbegin();
		for ( vector<Area*>::iterator freeSpace = diskUse.begin();
			freeSpace != diskUse.end() && moveTo != diskUse.rend();
			++freeSpace ) {
			if ( !( *freeSpace )->m_freeIndices.empty() ) {
				// free area
				vector<long> gone;
				size_t pos = freeSpace - diskUse.begin();
				for ( long blockIndex : ( *freeSpace )->m_freeIndices ) {
					if ( moveBlock(diskUse.rend(), moveTo, blockIndex) ) {
						gone.push_back(blockIndex);
					}
				}
				if ( gone.size() == ( *freeSpace )->m_freeIndices.size() ) {
					( *freeSpace )->m_freeIndices.clear();
				}
			}
		}
	}
	else {
		for ( int moveCandidateIndex = diskUse.size() - 1; moveCandidateIndex >= 0; --moveCandidateIndex ) {
			if ( diskUse[ moveCandidateIndex ]->m_usedIndices.empty() ) {
				continue; 
			}

			size_t needed = diskUse[ moveCandidateIndex ]->m_usedIndices.size();
			for ( size_t freeCheckIndex = 0; freeCheckIndex < moveCandidateIndex; ++freeCheckIndex ) {
				if ( diskUse[ freeCheckIndex ]->m_freeIndices.size() >= needed ) {
					for ( size_t i = 0; i < needed; ++i ) {
						diskUse[ moveCandidateIndex ]->m_usedIndices[ i ] = diskUse[ freeCheckIndex ]->m_freeIndices[ i ];
					}
					diskUse[ freeCheckIndex ]->m_freeIndices.erase(diskUse[ freeCheckIndex ]->m_freeIndices.begin(),
						diskUse[ freeCheckIndex ]->m_freeIndices.begin() + needed);
					break;
				}
			}
		}
	}

	for (auto& a : diskUse ) {
		if ( a->m_fileId >= 0 ) {
			t += a->checkSum();
		}
		delete a;
	}

	return t;
}

solveResult day9Solver::compute2() {
	s_part2 = true;
    return compute();
}

void day9Solver::loadTestData() {
  clearData();

    loadData("2333133121414131402");
}
